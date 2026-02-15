package grafica;

import grafica.storeData.CardImageResolver;
import grafica.storeData.UserPrefs;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Card;

import java.util.*;

public class OwnedCardsController {

    @FXML public ListView<Card> cardsList;
    @FXML public TextField searchField;

    private List<Card> allCards;
    private Set<String> ownedCardIds;
    private Runnable onBack;

    // cache immagini
    private final Map<String, Image> imageCache = new HashMap<>();

    // QUI ricordiamo cosa è stato deselezionato (persistente)
    private Set<String> deselectedCardIds = new HashSet<>();

    public void init(List<Card> allCards, Set<String> ownedCardIds, Runnable onBack) {
        this.allCards = (allCards == null) ? List.of() : allCards;
        this.ownedCardIds = ownedCardIds;
        this.onBack = onBack;

        // 1) Carico deselezioni salvate
        this.deselectedCardIds = UserPrefs.loadDeselectedCardIds();

        // 2) Default: tutte possedute, tranne quelle deselezionate in passato
        if (this.ownedCardIds != null) {
            this.ownedCardIds.clear();
            for (Card c : this.allCards) {
                if (c != null && c.getId() != null && !c.getId().isBlank()) {
                    this.ownedCardIds.add(c.getId().trim());
                }
            }
            this.ownedCardIds.removeAll(this.deselectedCardIds);
        }

        var items = FXCollections.observableArrayList(this.allCards);
        var filtered = new FilteredList<>(items, c -> true);
        cardsList.setItems(filtered);

        cardsList.setCellFactory(lv -> new ListCell<>() {

            private final ImageView iv = new ImageView();
            private final Label name = new Label();
            private final CheckBox cb = new CheckBox("Posseduta");

            private final VBox textBox = new VBox(6, name, cb);
            private final HBox row = new HBox(12, iv, textBox);

            {
                iv.setFitWidth(70);
                iv.setFitHeight(90);
                iv.setPreserveRatio(true);

                name.setStyle("-fx-font-weight: 900; -fx-text-fill: white;");

                HBox.setHgrow(textBox, Priority.ALWAYS);
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(Card card, boolean empty) {
                super.updateItem(card, empty);

                if (empty || card == null) {
                    setGraphic(null);
                    return;
                }

                String cardId = card.getId();

                name.setText(card.getName() == null ? "" : card.getName());

                Image img = loadCardImage(card);
                boolean hasImg = (img != null);
                iv.setImage(img);
                iv.setVisible(hasImg);
                iv.setManaged(hasImg);

                boolean owned = ownedCardIds != null && cardId != null && ownedCardIds.contains(cardId);

                // evita loop eventi
                cb.setOnAction(null);
                cb.setSelected(owned);

                cb.setOnAction(e -> {
                    if (ownedCardIds == null || cardId == null) return;

                    if (cb.isSelected()) {
                        // torna posseduta => rimuovi da deselected
                        ownedCardIds.add(cardId);
                        deselectedCardIds.remove(cardId);
                    } else {
                        // deselezionata => salva in deselected
                        ownedCardIds.remove(cardId);
                        deselectedCardIds.add(cardId);
                    }

                    // persiste subito (così “si ricorda” anche se chiudi)
                    UserPrefs.saveDeselectedCardIds(deselectedCardIds);
                });

                setGraphic(row);
            }
        });

        searchField.textProperty().addListener((obs, oldV, q) -> {
            String s = (q == null) ? "" : q.toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> {
                String n = (c.getName() == null) ? "" : c.getName().toLowerCase(Locale.ROOT);
                return n.contains(s);
            });
        });
    }

    private Image loadCardImage(Card c) {
        if (c == null || c.getId() == null) return null;

        String path = CardImageResolver.resolve(c.getId());
        if (path == null) return null;

        return imageCache.computeIfAbsent(path, p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }

    @FXML
    public void back() {
        if (onBack != null) onBack.run();
    }
}