package org.progettoFIA.gaclashfx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.progettoFIA.gaclashfx.storeData.UserPrefs;
import org.progettoFIA.gaclashfx.temp.Card;
import org.progettoFIA.gaclashfx.temp.CardRepository;

import java.util.*;

public class MainViewController {

    private static final int MAX_SUM = 8;

    @FXML public BorderPane root;

    // Advanced UI
    @FXML private Label advancedToggle;
    @FXML private VBox advancedBox;
    @FXML private ImageView kingExpertImage;

    // 4 select
    @FXML private ComboBox<String> airTroopsSelect;
    @FXML private ComboBox<String> buildingsSelect;
    @FXML private ComboBox<String> spellsSelect;
    @FXML private ComboBox<String> winconSelect;

    private List<ComboBox<String>> sumCombos;

    private final UserPrefs prefs = new UserPrefs();
    private final Set<String> selectedCardIds = new HashSet<>();

    // stato layout per pagine senza navbar
    private Node savedTop;
    private Node savedBottom;
    private Node savedCenter;

    // cache immagini carte
    private final Map<String, Image> imageCache = new HashMap<>();

    // guardie anti-loop
    private boolean enforcingSum = false;
    private boolean refreshingItems = false;

    @FXML
    private void initialize() {
        var is = MainApp.class.getResourceAsStream("/img/king_expert.png");
        if (is != null && kingExpertImage != null) {
            kingExpertImage.setImage(new Image(is));
        }

        sumCombos = List.of(airTroopsSelect, buildingsSelect, spellsSelect, winconSelect);

        // valori iniziali safe
        for (ComboBox<String> cb : sumCombos) {
            if (cb != null && cb.getValue() == null) cb.setValue("-");
        }

        setupSumConstraintWithDynamicItems();
        refreshAllComboItems(); // primo popolamento corretto
    }

    // --------------------------
    // VINCOLO: SOMMA <= 8 (ROBUSTO)
    // --------------------------
    private void setupSumConstraintWithDynamicItems() {

        for (ComboBox<String> cb : sumCombos) {
            if (cb == null) continue;

            // quando cambia un valore: se sfora -> revert, poi refresh items
            cb.valueProperty().addListener((obs, oldV, newV) -> {
                if (enforcingSum || refreshingItems) return;

                if (newV == null) {
                    enforcingSum = true;
                    Platform.runLater(() -> {
                        cb.setValue("-");
                        enforcingSum = false;
                        refreshAllComboItems();
                    });
                    return;
                }

                int sum = sumAll();
                if (sum > MAX_SUM) {
                    enforcingSum = true;
                    Platform.runLater(() -> {
                        cb.setValue(oldV == null ? "-" : oldV);
                        enforcingSum = false;
                        refreshAllComboItems();
                    });
                } else {
                    Platform.runLater(this::refreshAllComboItems);
                }
            });

            // quando apro il dropdown: aggiorno i valori disponibili
            cb.setOnShowing(e -> refreshComboItems(cb));
        }
    }

    private void refreshAllComboItems() {
        if (refreshingItems) return;
        refreshingItems = true;
        try {
            for (ComboBox<String> cb : sumCombos) {
                if (cb != null) refreshComboItems(cb);
            }
        } finally {
            refreshingItems = false;
        }
    }

    private void refreshComboItems(ComboBox<String> owner) {
        if (owner == null) return;

        int max = maxAllowedFor(owner);
        String current = owner.getValue();
        if (current == null) current = "-";

        // nuova lista: "-" + 0..max
        List<String> allowed = new ArrayList<>();
        allowed.add("-");
        for (int i = 0; i <= max; i++) allowed.add(String.valueOf(i));

        // se per qualche motivo il valore corrente non è nella lista, lo aggiungo
        if (!allowed.contains(current)) allowed.add(current);

        owner.setItems(FXCollections.observableArrayList(allowed));
        owner.setValue(current);
    }

    private int maxAllowedFor(ComboBox<String> owner) {
        int sumOthers = 0;
        for (ComboBox<String> cb : sumCombos) {
            if (cb == null) continue;
            if (cb == owner) continue;
            sumOthers += parseValue(cb.getValue());
        }
        int remaining = MAX_SUM - sumOthers;
        if (remaining < 0) remaining = 0;
        if (remaining > MAX_SUM) remaining = MAX_SUM;
        return remaining;
    }

    private int sumAll() {
        int s = 0;
        for (ComboBox<String> cb : sumCombos) {
            if (cb == null) continue;
            s += parseValue(cb.getValue());
        }
        return s;
    }

    private int parseValue(String v) {
        if (v == null) return 0;
        v = v.trim();
        if (v.isEmpty() || "-".equals(v)) return 0;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --------------------------
    // TOGGLE IMPOSTAZIONI AVANZATE
    // --------------------------
    @FXML
    private void toggleAdvanced() {
        boolean show = !advancedBox.isVisible();

        advancedBox.setVisible(show);
        advancedBox.setManaged(show);

        if (advancedToggle != null) {
            advancedToggle.setText(show ? "impostazioni avanzate ↑" : "impostazioni avanzate ↓");
        }

        if (kingExpertImage != null) {
            kingExpertImage.setVisible(!show);
            kingExpertImage.setManaged(!show);
        }
    }

    // --------------------------
    // NAVIGAZIONE SENZA NAVBAR
    // --------------------------
    private void saveLayoutStateIfNeeded() {
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

    // --------------------------
    // MENU: CARTE POSSEDUTE
    // --------------------------
    @FXML
    private void openOwnedCards() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/OwnedCardsView.fxml"));
            Parent view = loader.load();

            OwnedCardsController c = loader.getController();
            c.init(prefs, this::restoreLayoutState);

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
    public void GA(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/ResultDeckView.fxml"));
            Parent view = loader.load();

            saveLayoutStateIfNeeded();
            root.setTop(null);
            root.setBottom(null);
            root.setCenter(view);

        } catch (Exception ex) {
            ex.printStackTrace();
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
        if (oldId instanceof String) selectedCardIds.remove((String) oldId);

        Label plus = new Label("+");
        plus.getStyleClass().add("card-plus");
        slot.setGraphic(plus);

        slot.setTooltip(null);
        slot.getProperties().remove("selectedCardId");
    }

    private List<Card> getSelectableCards() {
        List<Card> all = CardRepository.getAll();
        if (prefs.ownedCardIds == null || prefs.ownedCardIds.isEmpty()) return all;

        return all.stream()
                .filter(c -> prefs.ownedCardIds.contains(c.getId()))
                .toList();
    }

    // --------------------------
    // POPUP SCELTA CARTA (IMMAGINI)
    // --------------------------
    private Card showPickCardPopup(List<Card> cards) {
        Dialog<Card> dialog = new Dialog<>();
        dialog.setTitle("Scegli una carta");
        dialog.setHeaderText("Scegli una carta");

        // Aggancia il dialog alla finestra principale (se disponibile)
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }

        ButtonType chooseBtn = new ButtonType("Seleziona", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(chooseBtn, ButtonType.CANCEL);

        // ====== STILE: usa lo stesso CSS della pagina + classi dedicate al popup
        DialogPane pane = dialog.getDialogPane();
        var cssUrl = MainApp.class.getResource("/MainView.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }
        pane.getStyleClass().add("cr-dialog");

        // Contenuto
        Label hint = new Label("Seleziona una carta:");
        hint.getStyleClass().add("cr-dialog-hint");

        TextField search = new TextField();
        search.setPromptText("Cerca...");
        search.getStyleClass().addAll("field-input", "cr-dialog-search");

        var items = FXCollections.observableArrayList(cards);
        var filtered = new FilteredList<>(items, x -> true);

        ListView<Card> listView = new ListView<>(filtered);
        listView.getStyleClass().add("cr-dialog-list");

        // cell con immagine + disabilitazione se già selezionata
        listView.setCellFactory(lv -> new ListCell<>() {
            private final ImageView iv = new ImageView();
            private final ColorAdjust gray = new ColorAdjust();

            {
                iv.setFitWidth(90);
                iv.setFitHeight(110);
                iv.setPreserveRatio(true);
                gray.setSaturation(-1);
            }

            @Override
            protected void updateItem(Card c, boolean empty) {
                super.updateItem(c, empty);

                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    setDisable(false);
                    setOpacity(1.0);
                    return;
                }

                Image img = loadCardImage(c);
                if (img != null) {
                    iv.setImage(img);
                    setGraphic(iv);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(c.getName());
                }

                boolean alreadyPicked = selectedCardIds.contains(c.getId());
                setDisable(alreadyPicked);

                if (alreadyPicked) {
                    iv.setEffect(gray);
                    setOpacity(0.55);
                } else {
                    iv.setEffect(null);
                    setOpacity(1.0);
                }

                setTooltip(new Tooltip(c.getName()));
            }
        });

        // filtro testuale
        search.textProperty().addListener((obs, o, q) -> {
            String s = (q == null) ? "" : q.toLowerCase();
            filtered.setPredicate(c -> c.getName().toLowerCase().contains(s));
        });

        VBox box = new VBox(10, hint, search, listView);
        box.getStyleClass().add("cr-dialog-content");
        box.setPrefSize(380, 560);
        pane.setContent(box);

        // Bottoni: style coerente pagina
        Button okBtn = (Button) pane.lookupButton(chooseBtn);
        okBtn.getStyleClass().add("cr-primary-btn");

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) cancelBtn.getStyleClass().add("cr-secondary-btn");

        // Disabilita OK se nulla selezionato
        okBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        // risultato
        dialog.setResultConverter(bt -> bt == chooseBtn ? listView.getSelectionModel().getSelectedItem() : null);

        // doppio click = conferma
        listView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    // --------------------------
    // SLOT: MOSTRA IMMAGINE
    // --------------------------
    private void setSlotCard(Button slot, Card card) {
        Object oldId = slot.getProperties().get("selectedCardId");
        if (oldId instanceof String) selectedCardIds.remove((String) oldId);

        Image img = loadCardImage(card);

        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(74);
            iv.setFitHeight(94);
            iv.setPreserveRatio(true);
            slot.setGraphic(iv);
            slot.setText(null);
        } else {
            Label name = new Label(card.getName());
            name.getStyleClass().add("card-name");
            name.setWrapText(true);
            name.setMaxWidth(70);
            slot.setGraphic(name);
            slot.setText(null);
        }

        slot.setTooltip(new Tooltip(card.getName()));
        slot.getProperties().put("selectedCardId", card.getId());
        selectedCardIds.add(card.getId());
    }

    // --------------------------
    // CARICAMENTO IMMAGINI CARTE
    // --------------------------
    private Image loadCardImage(Card c) {
        if (c.getImagePath() == null) return null;
        return imageCache.computeIfAbsent(c.getImagePath(), p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }
}