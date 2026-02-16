package grafica;

import agente.Genetic_Algoritm.GA_core;
import agente.Genetic_Algoritm.individuals.Deck;
import agente.Genetic_Algoritm.individuals.DeckConstraints;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import metriche.EvaluationMetrics;
import model.Card;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultDeckController {

    // --- 8 slot GA
    @FXML private ImageView card0Img;
    @FXML private ImageView card1Img;
    @FXML private ImageView card2Img;
    @FXML private ImageView card3Img;
    @FXML private ImageView card4Img;
    @FXML private ImageView card5Img;
    @FXML private ImageView card6Img;
    @FXML private ImageView card7Img;

    @FXML private Label card0Label;
    @FXML private Label card1Label;
    @FXML private Label card2Label;
    @FXML private Label card3Label;
    @FXML private Label card4Label;
    @FXML private Label card5Label;
    @FXML private Label card6Label;
    @FXML private Label card7Label;

    // --- 8 slot SA
    @FXML private ImageView SAcard0Img;
    @FXML private ImageView SAcard1Img;
    @FXML private ImageView SAcard2Img;
    @FXML private ImageView SAcard3Img;
    @FXML private ImageView SAcard4Img;
    @FXML private ImageView SAcard5Img;
    @FXML private ImageView SAcard6Img;
    @FXML private ImageView SAcard7Img;

    @FXML private Label SAcard0Label;
    @FXML private Label SAcard1Label;
    @FXML private Label SAcard2Label;
    @FXML private Label SAcard3Label;
    @FXML private Label SAcard4Label;
    @FXML private Label SAcard5Label;
    @FXML private Label SAcard6Label;
    @FXML private Label SAcard7Label;

    // --- Dettagli su pannello (Label)
    @FXML private Label detailsLabel;     // GA details
    @FXML private Label SAdetailsLabel;   // SA details

    // --- Toggle state + frecce + box da collassare
    @FXML private Label gaDetailsArrow;
    @FXML private Label saDetailsArrow;
    @FXML private VBox gaDetailsBox;
    @FXML private VBox saDetailsBox;

    @FXML private TextArea notesArea;
    @FXML private Label statusLabel;

    // ===== METRICHE (NUOVO) =====
    @FXML private VBox metricsBox;
    @FXML private BarChart<String, Number> convChart; // Convergenza g*
    @FXML private BarChart<String, Number> cvChart;   // CardUsageCV

    private static final double METRICS_ALPHA = 0.95;
    private static final int METRICS_PATIENCE = 5;

    private Runnable onBack;

    private boolean gaDetailsVisible = true;
    private boolean saDetailsVisible = true;

    private final Map<String, Image> imageCache = new HashMap<>();

    // fallback 10-img
    private static final Map<String, String> CARD_IMG_BY_ID = Map.ofEntries(
            Map.entry("hog_rider", "/img/c01.png"),
            Map.entry("the_log", "/img/c02.png"),
            Map.entry("fireball", "/img/c03.png"),
            Map.entry("musketeer", "/img/c04.png"),
            Map.entry("golem", "/img/c05.png"),
            Map.entry("miner", "/img/c06.png"),
            Map.entry("poison", "/img/c07.png"),
            Map.entry("skeletons", "/img/c08.png"),
            Map.entry("zap", "/img/c09.png"),
            Map.entry("tesla", "/img/c10.png")
    );

    // --- SA (reflection)
    private static final String SA_CORE_CLASS = "agente.Simulated_Annealing.SA_core";
    private static final String SA_VINCOLI_CLASS = "agente.Simulated_Annealing.Stato_corrente.Vincoli";

    private record Both(Object gaOut, Object saOut, String saError) {}

    @FXML
    private void initialize() {
        applyGADetailsVisibility(false);
        applySADetailsVisibility(false);

        // NUOVO: nascondo metriche finché non ho i risultati
        if (metricsBox != null) {
            metricsBox.setVisible(false);
            metricsBox.setManaged(false);
        }
    }

    public void init(List<Card> pool,
                     DeckConstraints constraints,
                     double delta,
                     double desiredAvgElixir,
                     Runnable onBack) {

        this.onBack = onBack;

        if (detailsLabel != null) detailsLabel.setText("Genero GA...");
        if (SAdetailsLabel != null) SAdetailsLabel.setText("In attesa del SA...");
        if (statusLabel != null) statusLabel.setText("Avvio...");

        applyGADetailsVisibility(false);
        applySADetailsVisibility(false);

        if (metricsBox != null) {
            metricsBox.setVisible(false);
            metricsBox.setManaged(false);
        }

        Task<Both> task = new Task<>() {
            @Override
            protected Both call() {
                // 1) GA (con trace per metriche)
                updateMessage("Genero GA...");
                Object gaOut;
                try {
                    gaOut = GA_core.runWithTrace(pool, constraints, delta, desiredAvgElixir);
                } catch (Throwable t) {
                    // fallback: se non esiste runWithTrace
                    gaOut = GA_core.run(pool, constraints, delta, desiredAvgElixir);
                }

                // 2) SA (provo runWithTrace, altrimenti run)
                updateMessage("Genero SA...");
                try {
                    Object saOut = runSAWithOptionalTrace(pool, constraints, delta, desiredAvgElixir);
                    return new Both(gaOut, saOut, null);
                } catch (Exception ex) {
                    return new Both(gaOut, null, ex.getMessage());
                }
            }
        };

        if (statusLabel != null) statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(ev -> {
            if (statusLabel != null) statusLabel.textProperty().unbind();

            Both both = task.getValue();

            // --- render GA
            Deck gaBestDeck = null;
            String gaDetails = null;

            if (both != null && both.gaOut != null) {
                try {
                    Object bd = tryInvoke(both.gaOut, "bestDeck");
                    if (bd instanceof Deck d) gaBestDeck = d;

                    Object det = tryInvoke(both.gaOut, "details");
                    gaDetails = (det != null) ? String.valueOf(det) : null;
                } catch (Exception ignored) {}
            }

            if (gaBestDeck == null) {
                if (detailsLabel != null) detailsLabel.setText(gaDetails != null ? safeText(gaDetails) : "Errore: output GA nullo");
            } else {
                renderDeckGA(gaBestDeck);
                if (detailsLabel != null) detailsLabel.setText(safeText(gaDetails));
            }

            // --- render SA
            if (both == null) {
                if (SAdetailsLabel != null) SAdetailsLabel.setText("Errore: output SA nullo");
            } else if (both.saOut == null) {
                if (SAdetailsLabel != null) {
                    SAdetailsLabel.setText("Errore durante SA:\n" + (both.saError != null ? both.saError : "(sconosciuto)"));
                }
            } else {
                try {
                    List<Card> saCards = extractCardsFromSAOutput(both.saOut);
                    String saDetails = extractDetailsFromSAOutput(both.saOut);

                    if (saCards != null && saCards.size() == 8) {
                        renderCardsIntoSlots(saCards,
                                new ImageView[]{SAcard0Img, SAcard1Img, SAcard2Img, SAcard3Img, SAcard4Img, SAcard5Img, SAcard6Img, SAcard7Img},
                                new Label[]{SAcard0Label, SAcard1Label, SAcard2Label, SAcard3Label, SAcard4Label, SAcard5Label, SAcard6Label, SAcard7Label}
                        );
                    } else {
                        if (SAdetailsLabel != null) SAdetailsLabel.setText("SA: impossibile estrarre 8 carte dallo stato.");
                    }

                    if (SAdetailsLabel != null && saDetails != null) {
                        SAdetailsLabel.setText(safeText(saDetails));
                    }

                } catch (Exception ex) {
                    if (SAdetailsLabel != null) SAdetailsLabel.setText("Errore render SA:\n" + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            // ===== METRICHE (NUOVO) =====
            tryRenderMetrics(both, pool);

            if (statusLabel != null) statusLabel.setText("Pronto");
        });

        task.setOnFailed(ev -> {
            if (statusLabel != null) statusLabel.textProperty().unbind();
            Throwable ex = task.getException();

            String msg = "Errore:\n" + (ex != null ? ex.getMessage() : "(sconosciuto)");
            if (detailsLabel != null) detailsLabel.setText(msg);
            if (SAdetailsLabel != null) SAdetailsLabel.setText(msg);
            if (statusLabel != null) statusLabel.setText("Errore");
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(task, "ga-sa-runner");
        t.setDaemon(true);
        t.start();
    }

    private String safeText(String s) {
        if (s == null) return "";
        return s.replace("\t", "    ");
    }

    @FXML
    private void back() {
        if (onBack != null) onBack.run();
    }

    // =========================
    // TOGGLE DETTAGLI (GA / SA)
    // =========================

    @FXML
    private void toggleGADetails() { applyGADetailsVisibility(!gaDetailsVisible); }

    @FXML
    private void toggleSADetails() { applySADetailsVisibility(!saDetailsVisible); }

    private void applyGADetailsVisibility(boolean show) {
        gaDetailsVisible = show;
        if (gaDetailsBox != null) {
            gaDetailsBox.setVisible(show);
            gaDetailsBox.setManaged(show);
        }
        if (gaDetailsArrow != null) gaDetailsArrow.setText(show ? "▲" : "▼");
    }

    private void applySADetailsVisibility(boolean show) {
        saDetailsVisible = show;
        if (saDetailsBox != null) {
            saDetailsBox.setVisible(show);
            saDetailsBox.setManaged(show);
        }
        if (saDetailsArrow != null) saDetailsArrow.setText(show ? "▲" : "▼");
    }

    // -------------------
    // RENDER GA / SA
    // -------------------

    private void renderDeckGA(Deck deck) {
        List<Card> cards = deck.getCards();
        renderCardsIntoSlots(cards,
                new ImageView[]{card0Img, card1Img, card2Img, card3Img, card4Img, card5Img, card6Img, card7Img},
                new Label[]{card0Label, card1Label, card2Label, card3Label, card4Label, card5Label, card6Label, card7Label}
        );
    }

    private void renderCardsIntoSlots(List<Card> cards, ImageView[] imgs, Label[] labels) {
        for (int i = 0; i < 8; i++) {
            Card c = cards.get(i);

            Image img = loadCardImage(c);
            if (imgs[i] != null) imgs[i].setImage(img);

            boolean hasImg = (img != null);

            if (imgs[i] != null) {
                imgs[i].setVisible(hasImg);
                imgs[i].setManaged(hasImg);
            }
            if (labels[i] != null) {
                labels[i].setText(c.getName());
                labels[i].setVisible(!hasImg);
                labels[i].setManaged(!hasImg);
            }
        }
    }

    private Image loadCardImage(Card c) {
        if (c == null || c.getId() == null) return null;

        String id = c.getId();

        String[] candidates = {
                "/img/immagini carte/" + id + ".png",
                "/img/immagini carte/" + id.replace('_', '-') + ".png",
                "/img/" + id + ".png"
        };

        for (String p : candidates) {
            var is = MainApp.class.getResourceAsStream(p);
            if (is != null) return new Image(is);
        }

        String fallback = CARD_IMG_BY_ID.get(id);
        if (fallback == null) return null;

        return imageCache.computeIfAbsent(fallback, p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }

    // -------------------
    // METRICHE (NUOVO)
    // -------------------

    private void tryRenderMetrics(Both both, List<Card> universe) {
        if (metricsBox == null || convChart == null || cvChart == null) return;
        if (both == null || both.gaOut == null || both.saOut == null) return;

        try {
            // GA: curve + decks
            @SuppressWarnings("unchecked")
            List<Double> gaCurve = (List<Double>) tryInvoke(both.gaOut, "bestFitnessPerGen");
            @SuppressWarnings("unchecked")
            List<Deck> gaDecks = (List<Deck>) tryInvoke(both.gaOut, "finalPopulation");

            // SA: curve + decks
            @SuppressWarnings("unchecked")
            List<Double> saCurve = (List<Double>) tryInvoke(both.saOut, "bestUtilityPerStep");
            @SuppressWarnings("unchecked")
            List<Deck> saDecks = (List<Deck>) tryInvoke(both.saOut, "bestDeckPerStep");

            if (gaCurve == null || saCurve == null || gaDecks == null || saDecks == null) {
                // se non avete implementato runWithTrace nei core, qui non arrivano le info
                return;
            }

            int gStarGA = EvaluationMetrics.convergenceGeneration(gaCurve, METRICS_ALPHA, METRICS_PATIENCE);
            int gStarSA = EvaluationMetrics.convergenceGeneration(saCurve, METRICS_ALPHA, METRICS_PATIENCE);

            double cvGA = EvaluationMetrics.cardUsageCV(gaDecks, universe);
            double cvSA = EvaluationMetrics.cardUsageCV(saDecks, universe);

            fillCompareChart(convChart, "g*", gStarGA, gStarSA);
            fillCompareChart(cvChart, "CV", cvGA, cvSA);

            metricsBox.setVisible(true);
            metricsBox.setManaged(true);

        } catch (Exception ignored) {
            // se qualcosa va storto, semplicemente non mostro le metriche
        }
    }

    private void fillCompareChart(BarChart<String, Number> chart, String category, double gaValue, double saValue) {
        chart.getData().clear();

        XYChart.Series<String, Number> sGA = new XYChart.Series<>();
        sGA.setName("GA");
        sGA.getData().add(new XYChart.Data<>(category, gaValue));

        XYChart.Series<String, Number> sSA = new XYChart.Series<>();
        sSA.setName("SA");
        sSA.getData().add(new XYChart.Data<>(category, saValue));

        chart.getData().addAll(sGA, sSA);
    }

    // -------------------
    // SA CALL (reflection)
    // -------------------

    private Object runSAWithOptionalTrace(List<Card> pool, DeckConstraints constraints, double delta, double desiredAvgElixir) throws Exception {
        Class<?> saCore = Class.forName(SA_CORE_CLASS);

        // 0) Provo runWithTrace(List<Card>, Vincoli, double, double)
        try {
            Object vincoli = buildVincoliFromDeckConstraints(constraints);
            Method m0 = saCore.getMethod("runWithTrace", List.class, vincoli.getClass(), double.class, double.class);
            return m0.invoke(null, pool, vincoli, delta, desiredAvgElixir);
        } catch (NoSuchMethodException ignored) {}

        // 1) Provo run(List<Card>, DeckConstraints, double, double)
        try {
            Method m = saCore.getMethod("run", List.class, constraints.getClass(), double.class, double.class);
            return m.invoke(null, pool, constraints, delta, desiredAvgElixir);
        } catch (NoSuchMethodException ignored) {}

        // 2) Provo run(List<Card>, Vincoli, double, double)
        Object vincoli = buildVincoliFromDeckConstraints(constraints);
        Method m2 = saCore.getMethod("run", List.class, vincoli.getClass(), double.class, double.class);
        return m2.invoke(null, pool, vincoli, delta, desiredAvgElixir);
    }

    private Object buildVincoliFromDeckConstraints(DeckConstraints dc) throws Exception {
        Class<?> vincoliCls = Class.forName(SA_VINCOLI_CLASS);
        Object v = vincoliCls.getDeclaredConstructor().newInstance();

        setFieldIfExists(v, "mandatoryCardsId", dc.mandatoryCardsId);
        setFieldIfExists(v, "nSpells", dc.nSpells);
        setFieldIfExists(v, "nBuildings", dc.nBuildings);
        setFieldIfExists(v, "nFlyingTroop", dc.nFlyingTroop);
        setFieldIfExists(v, "nBuildingTarget", dc.nBuildingTarget);

        return v;
    }

    private void setFieldIfExists(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {}
    }

    private List<Card> extractCardsFromSAOutput(Object saOut) throws Exception {
        Object best = tryInvoke(saOut, "bestState", "bestStato", "best", "bestSolution", "bestDeck");
        if (best == null) return null;

        Object cards = tryInvoke(best, "getCards", "cards");
        if (cards instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Card> list = (List<Card>) cards;
            return list;
        }
        return null;
    }

    private String extractDetailsFromSAOutput(Object saOut) {
        try {
            Object d = tryInvoke(saOut, "details", "getDetails");
            return (d != null) ? String.valueOf(d) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object tryInvoke(Object target, String... names) throws Exception {
        for (String n : names) {
            try {
                Method m = target.getClass().getMethod(n);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
}
