package org.progettoFIA.gaclashfx;


import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import org.progettoFIA.gaclashfx.storeData.UserPrefs;
import org.progettoFIA.gaclashfx.temp.Card;
import org.progettoFIA.gaclashfx.temp.CardRepository;

public class OwnedCardsController {
    public ListView<Card> cardsList;
    public TextField searchField;

    private UserPrefs prefs;
    private Runnable onBack;

    public void init(UserPrefs prefs, Runnable onBack) {
        this.prefs = prefs;
        this.onBack = onBack;

        var allCards = CardRepository.getAll(); // o iniettato
        var items = javafx.collections.FXCollections.observableArrayList(allCards);
        cardsList.setItems(items);

        cardsList.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox cb = new CheckBox();
            @Override protected void updateItem(Card card, boolean empty) {
                super.updateItem(card, empty);
                if (empty || card == null) { setGraphic(null); return; }

                cb.setText(card.getName());
                cb.setSelected(prefs.ownedCardIds.contains(card.getId()));
                cb.setOnAction(e -> {
                    if (cb.isSelected()) prefs.ownedCardIds.add(card.getId());
                    else prefs.ownedCardIds.remove(card.getId());
                });
                setGraphic(cb);
            }
        });

        // filtro ricerca (incrementale, opzionale)
        searchField.textProperty().addListener((obs, o, q) -> {
            String s = q == null ? "" : q.toLowerCase();
            cardsList.setItems(items.filtered(c -> c.getName().toLowerCase().contains(s)));
        });
    }

    public void back() { onBack.run(); }
}

