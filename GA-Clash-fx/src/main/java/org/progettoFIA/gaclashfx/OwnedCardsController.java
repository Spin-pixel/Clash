package org.progettoFIA.gaclashfx;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import org.progettoFIA.gaclashfx.storeData.UserPrefs;
import org.progettoFIA.gaclashfx.temp.Card;
import org.progettoFIA.gaclashfx.temp.CardRepository;

import java.util.HashMap;
import java.util.Map;

public class OwnedCardsController {

    @FXML public ListView<Card> cardsList;
    @FXML public TextField searchField;

    private UserPrefs prefs;
    private Runnable onBack;

    // cache immagini per non ricaricarle ogni scroll
    private final Map<String, Image> imageCache = new HashMap<>();

    public void init(UserPrefs prefs, Runnable onBack) {
        this.prefs = prefs;
        this.onBack = onBack;

        var allCards = CardRepository.getAll();
        var items = FXCollections.observableArrayList(allCards);
        var filtered = new FilteredList<>(items, c -> true);

        cardsList.setItems(filtered);

        cardsList.setCellFactory(lv -> new ListCell<>() {

            private final ImageView iv = new ImageView();
            private final Label name = new Label();
            private final CheckBox cb = new CheckBox("Posseduta");
            private final VBox box = new VBox(6, iv, name, cb);

            {
                iv.setFitWidth(120);
                iv.setFitHeight(150);
                iv.setPreserveRatio(true);

                name.setStyle("-fx-font-weight: 900;");

                // evita che la cella si restringa
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(Card card, boolean empty) {
                super.updateItem(card, empty);

                if (empty || card == null) {
                    setGraphic(null);
                    return;
                }

                name.setText(card.getName());

                Image img = loadCardImage(card);
                iv.setImage(img);

                boolean owned = prefs.ownedCardIds.contains(card.getId());
                cb.setSelected(owned);

                cb.setOnAction(e -> {
                    if (cb.isSelected()) prefs.ownedCardIds.add(card.getId());
                    else prefs.ownedCardIds.remove(card.getId());
                });

                setGraphic(box);
            }
        });

        // filtro ricerca (senza ricreare l'intera lista)
        searchField.textProperty().addListener((obs, oldV, q) -> {
            String s = (q == null) ? "" : q.toLowerCase();
            filtered.setPredicate(c -> c.getName().toLowerCase().contains(s));
        });
    }

    private Image loadCardImage(Card c) {
        if (c.getImagePath() == null) return null;

        return imageCache.computeIfAbsent(c.getImagePath(), p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }

    @FXML
    public void back() {
        if (onBack != null) onBack.run();
    }
}
