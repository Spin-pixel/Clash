package grafica;

import agente.GA.GA_core;
import agente.GA.individuals.Deck;
import agente.GA.individuals.DeckConstraints;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultDeckController {

    // 8 slot (img + label fallback)
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

    @FXML private TextArea detailsArea;
    @FXML private TextArea notesArea;
    @FXML private Label statusLabel;

    private Runnable onBack;

    private final Map<String, Image> imageCache = new HashMap<>();

    // stesso fallback 10-img
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

    public void init(List<Card> pool,
                     DeckConstraints constraints,
                     double delta,
                     double desiredAvgElixir,
                     Runnable onBack) {

        this.onBack = onBack;

        if (statusLabel != null) statusLabel.setText("Genero il mazzo...");

        Task<GA_core.Output> task = new Task<>() {
            @Override
            protected GA_core.Output call() {
                return GA_core.run(pool, constraints, delta, desiredAvgElixir);
            }
        };

        task.setOnSucceeded(ev -> {
            GA_core.Output out = task.getValue();
            if (out == null || out.bestDeck() == null) {
                if (detailsArea != null) detailsArea.setText(out != null ? out.details() : "Errore: output nullo");
                if (statusLabel != null) statusLabel.setText("Errore");
                return;
            }

            renderDeck(out.bestDeck());

            if (detailsArea != null) detailsArea.setText(out.details());
            if (statusLabel != null) statusLabel.setText("Pronto");
        });

        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            if (detailsArea != null) {
                detailsArea.setText("Errore durante il GA:\n" + (ex != null ? ex.getMessage() : "(sconosciuto)"));
            }
            if (statusLabel != null) statusLabel.setText("Errore");
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(task, "ga-runner");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void back() {
        if (onBack != null) onBack.run();
    }

    private void renderDeck(Deck deck) {
        List<Card> cards = deck.getCards();

        ImageView[] imgs = {card0Img, card1Img, card2Img, card3Img, card4Img, card5Img, card6Img, card7Img};
        Label[] labels = {card0Label, card1Label, card2Label, card3Label, card4Label, card5Label, card6Label, card7Label};

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

        // prova immagini per-id se le aggiungi in futuro
        String[] candidates = {
                "/img/immagini carte/" + id + ".png",
                "/img/immagini carte/" + id.replace('_', '-') + ".png",
                "/img/" + id + ".png"
        };

        for (String p : candidates) {
            var is = MainApp.class.getResourceAsStream(p);
            if (is != null) return new Image(is);
        }

        // fallback 10 immagini
        String fallback = CARD_IMG_BY_ID.get(id);
        if (fallback == null) return null;

        return imageCache.computeIfAbsent(fallback, p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }
}
