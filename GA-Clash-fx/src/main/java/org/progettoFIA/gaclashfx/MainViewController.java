package org.progettoFIA.gaclashfx;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.progettoFIA.gaclashfx.storeData.UserPrefs;
import org.progettoFIA.gaclashfx.temp.Card;
import org.progettoFIA.gaclashfx.temp.CardRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MainViewController {

    @FXML public BorderPane root;

    private final UserPrefs prefs = new UserPrefs();
    private final Set<String> selectedCardIds = new HashSet<>();

    // Per gestire "pagine" senza navbar: salviamo stato e lo ripristiniamo
    private Node savedTop;
    private Node savedBottom;
    private Node savedCenter;

    // --------------------------
    // NAVIGAZIONE SENZA NAVBAR
    // --------------------------

    private void saveLayoutStateIfNeeded() {
        // Salva solo se non già salvato (evita overwrite in catene di navigazione)
        if (savedCenter == null) {
            savedTop = root.getTop();
            savedBottom = root.getBottom();
            savedCenter = root.getCenter();
        }
    }

    private void restoreLayoutState() {
        root.setTop(savedTop);
        root.setBottom(savedBottom);
        root.setCenter(savedCenter);

        savedTop = null;
        savedBottom = null;
        savedCenter = null;
    }

    private void showPageWithoutNavbar(Parent view, Runnable onBackAction) {
        saveLayoutStateIfNeeded();

        // Nasconde navbar
        root.setTop(null);
        root.setBottom(null);

        // Mostra pagina
        root.setCenter(view);

        // Se ti serve, puoi collegare onBackAction nel controller della pagina.
        // Qui lo usiamo passando restoreLayoutState() quando vuoi tornare indietro.
        if (onBackAction != null) onBackAction.run();
    }

    // --------------------------
    // MENU: CARTE POSSEDUTE
    // --------------------------

    @FXML
    private void openOwnedCards() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/OwnedCardsView.fxml"));
            Parent view = loader.load();

            OwnedCardsController c = loader.getController();
            // Back: ripristina navbar + center originale
            c.init(prefs, this::restoreLayoutState);

            // Salva layout e mostra pagina senza navbar
            saveLayoutStateIfNeeded();
            root.setTop(null);
            root.setBottom(null);
            root.setCenter(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // BOTTONE: GA -> RISULTATO
    // --------------------------

    @FXML
    public void GA() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/ResultDeckView.fxml"));
            Parent view = loader.load();

            ResultDeckController c = loader.getController();
            // Se nel ResultDeck hai un bottone "Back", fagli chiamare restoreLayoutState()
            // (aggiungi un metodo init(...) anche lì, se non lo hai)
            // Esempio:
            // c.init(this::restoreLayoutState);

            saveLayoutStateIfNeeded();
            root.setTop(null);
            root.setBottom(null);
            root.setCenter(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // SLOT CARTE: PICK / CLEAR
    // --------------------------

    @FXML
    private void pickCard(ActionEvent e) {
        Button slot = (Button) e.getSource();

        if (isSlotSelected(slot)) {
            clearSlotCard(slot);
            return;
        }

        List<Card> pool = getSelectableCards();
        Card chosen = showPickCardPopup(pool);

        if (chosen != null) {
            setSlotCard(slot, chosen);
        }
    }

    private boolean isSlotSelected(Button slot) {
        return slot.getProperties().containsKey("selectedCardId");
    }

    private void clearSlotCard(Button slot) {
        Object oldId = slot.getProperties().get("selectedCardId");
        if (oldId instanceof String) {
            selectedCardIds.remove((String) oldId);
        }

        Label plus = new Label("+");
        plus.getStyleClass().add("card-plus");
        slot.setGraphic(plus);

        slot.setTooltip(null);
        slot.getProperties().remove("selectedCardId");
    }

    private List<Card> getSelectableCards() {
        List<Card> all = CardRepository.getAll();
        if (prefs.ownedCardIds == null || prefs.ownedCardIds.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(c -> prefs.ownedCardIds.contains(c.getId()))
                .toList();
    }

    private Card showPickCardPopup(List<Card> cards) {
        Dialog<Card> dialog = new Dialog<>();
        dialog.setTitle("Scegli una carta");

        ButtonType chooseBtn = new ButtonType("Seleziona", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(chooseBtn, ButtonType.CANCEL);

        TextField search = new TextField();
        search.setPromptText("Cerca...");

        var items = FXCollections.observableArrayList(cards);
        var filtered = new FilteredList<>(items, x -> true);

        ListView<Card> listView = new ListView<>(filtered);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Card c, boolean empty) {
                super.updateItem(c, empty);

                if (empty || c == null) {
                    setText(null);
                    setDisable(false);
                    setTextFill(Color.BLACK);
                    setOpacity(1.0);
                    return;
                }

                setText(c.getName());

                boolean alreadyPicked = selectedCardIds.contains(c.getId());
                setDisable(alreadyPicked);

                if (alreadyPicked) {
                    setTextFill(Color.GRAY);
                    setOpacity(0.65);
                } else {
                    setTextFill(Color.BLACK);
                    setOpacity(1.0);
                }
            }
        });

        search.textProperty().addListener((obs, o, q) -> {
            String s = (q == null) ? "" : q.toLowerCase();
            filtered.setPredicate(c -> c.getName().toLowerCase().contains(s));
        });

        VBox box = new VBox(10, new Label("Seleziona una carta:"), search, listView);
        box.setPrefSize(320, 420);
        dialog.getDialogPane().setContent(box);

        Node ok = dialog.getDialogPane().lookupButton(chooseBtn);
        ok.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(bt -> bt == chooseBtn ? listView.getSelectionModel().getSelectedItem() : null);

        listView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        Optional<Card> res = dialog.showAndWait();
        return res.orElse(null);
    }

    private void setSlotCard(Button slot, Card card) {
        Object oldId = slot.getProperties().get("selectedCardId");
        if (oldId instanceof String) {
            selectedCardIds.remove((String) oldId);
        }

        Label name = new Label(card.getName());
        name.getStyleClass().add("card-name");
        name.setWrapText(true);
        name.setMaxWidth(70);

        slot.setGraphic(name);
        slot.setText(null);
        slot.setTooltip(new Tooltip(card.getName()));

        slot.getProperties().put("selectedCardId", card.getId());
        selectedCardIds.add(card.getId());
    }
}
