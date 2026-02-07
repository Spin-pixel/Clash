package org.progettoFIA.gaclashfx;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.ProgettoFIA.gacore.api.StopToken;
import org.ProgettoFIA.gacore.core.GAConfig;
import org.ProgettoFIA.gacore.core.SimpleBinaryGA;

public class MainView {

    private final BorderPane root = new BorderPane();

    private final XYChart.Series<Number, Number> bestSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> avgSeries = new XYChart.Series<>();

    private final Spinner<Integer> popSize = new Spinner<>(10, 1000, 80, 10);
    private final Spinner<Integer> chromLen = new Spinner<>(8, 256, 32, 8);
    private final Spinner<Integer> maxGen = new Spinner<>(10, 5000, 300, 50);
    private final Spinner<Integer> tournamentK = new Spinner<>(2, 15, 3, 1);

    private final Slider crossoverRate = new Slider(0, 1, 0.8);
    private final Slider mutationRate = new Slider(0, 0.2, 0.01);

    private final Spinner<Integer> elitism = new Spinner<>(0, 50, 1, 1);
    private final TextField seed = new TextField("123");

    private final Button startBtn = new Button("Start");
    private final Button stopBtn = new Button("Stop");

    private final Label status = new Label("Pronto.");
    private final Label bestGenome = new Label("-");
    private final Label bestFitness = new Label("-");

    private Task<Void> runningTask;
    private StopToken stopToken;

    public MainView() {
        root.setPadding(new Insets(12));
        root.setLeft(buildControls());
        root.setCenter(buildChart());
        root.setBottom(buildStatusBar());

        stopBtn.setDisable(true);
        startBtn.setOnAction(e -> start());
        stopBtn.setOnAction(e -> stop());
    }

    public Parent root() { return root; }

    private Parent buildControls() {
        crossoverRate.setShowTickMarks(true);
        crossoverRate.setShowTickLabels(true);
        crossoverRate.setMajorTickUnit(0.25);

        mutationRate.setShowTickMarks(true);
        mutationRate.setShowTickLabels(true);
        mutationRate.setMajorTickUnit(0.05);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int r = 0;
        grid.add(new Label("Population size"), 0, r); grid.add(popSize, 1, r++);
        grid.add(new Label("Chromosome length"), 0, r); grid.add(chromLen, 1, r++);
        grid.add(new Label("Max generations"), 0, r); grid.add(maxGen, 1, r++);
        grid.add(new Label("Tournament k"), 0, r); grid.add(tournamentK, 1, r++);
        grid.add(new Label("Elitism count"), 0, r); grid.add(elitism, 1, r++);
        grid.add(new Label("Random seed"), 0, r); grid.add(seed, 1, r++);

        grid.add(new Label("Crossover rate"), 0, r); grid.add(crossoverRate, 1, r++);
        grid.add(new Label("Mutation rate / gene"), 0, r); grid.add(mutationRate, 1, r++);

        HBox buttons = new HBox(10, startBtn, stopBtn);

        VBox box = new VBox(12,
                new Label("Parametri"),
                grid,
                buttons,
                new Separator(),
                new Label("Best genome:"),
                bestGenome,
                new Label("Best fitness:"),
                bestFitness
        );
        box.setPadding(new Insets(0, 10, 0, 0));
        box.setPrefWidth(320);
        return box;
    }

    private Parent buildChart() {
        NumberAxis x = new NumberAxis();
        x.setLabel("Generazione");

        NumberAxis y = new NumberAxis();
        y.setLabel("Fitness (OneMax)");

        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setCreateSymbols(false);

        bestSeries.setName("Best (global)");
        avgSeries.setName("Average");

        chart.getData().add(bestSeries);
        chart.getData().add(avgSeries);

        return chart;
    }

    private Parent buildStatusBar() {
        HBox bar = new HBox(12, status);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    private void start() {
        if (runningTask != null && runningTask.isRunning()) return;

        bestSeries.getData().clear();
        avgSeries.getData().clear();
        bestGenome.setText("-");
        bestFitness.setText("-");

        long parsedSeed;
        try {
            parsedSeed = Long.parseLong(seed.getText().trim());
        } catch (Exception ex) {
            status.setText("Seed non valido.");
            return;
        }

        GAConfig cfg = new GAConfig(
                popSize.getValue(),
                chromLen.getValue(),
                tournamentK.getValue(),
                crossoverRate.getValue(),
                mutationRate.getValue(),
                elitism.getValue(),
                maxGen.getValue(),
                parsedSeed
        );

        stopToken = new StopToken();
        SimpleBinaryGA ga = new SimpleBinaryGA();

        runningTask = new Task<>() {
            @Override
            protected Void call() {
                statusOnFx("Esecuzione...");
                ga.run(cfg, stopToken, stats -> {
                    // UI update: deve essere sul JavaFX Application Thread
                    Platform.runLater(() -> {
                        bestSeries.getData().add(new XYChart.Data<>(stats.generation(), stats.bestFitness()));
                        avgSeries.getData().add(new XYChart.Data<>(stats.generation(), stats.avgFitness()));
                        bestGenome.setText(stats.bestGenome());
                        bestFitness.setText(String.valueOf(stats.bestFitness()));
                        status.setText("Gen " + stats.generation() + " | best=" + stats.bestFitness());
                    });
                });
                statusOnFx("Finito.");
                return null;
            }
        };

        runningTask.setOnSucceeded(e -> onFinish());
        runningTask.setOnCancelled(e -> { status.setText("Interrotto."); onFinish(); });
        runningTask.setOnFailed(e -> { status.setText("Errore: " + runningTask.getException()); onFinish(); });

        startBtn.setDisable(true);
        stopBtn.setDisable(false);

        Thread t = new Thread(runningTask, "ga-worker");
        t.setDaemon(true);
        t.start();
    }

    private void stop() {
        if (stopToken != null) stopToken.requestStop();
        if (runningTask != null) runningTask.cancel();
    }

    private void onFinish() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    private void statusOnFx(String msg) {
        Platform.runLater(() -> status.setText(msg));
    }
}
