package grafica;

import agente.individuals.DeckConstraints;
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
import model.Card;
import service.CardService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MainViewController {

    private static final int MAX_SUM = 8;

    // nel tuo progetto il json sta qui (non in resources)
    private static final String[] CARDLIST_CANDIDATES = {
            "src/main/java/cardList.json",
            "cardList.json"
    };

    // fallback: nel progetto attuale ci sono solo 10 immagini (c01..c10)
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

    @FXML public BorderPane root;

    // Advanced UI
    @FXML private Label advancedToggle;
    @FXML private VBox advancedBox;
    @FXML private ImageView kingExpertImage;

    // Advanced params
    @FXML private Spinner<Double> deltaSpinner;
    @FXML private Spinner<Double> avgElixirSpinner;

    // 4 select
    @FXML private ComboBox<String> airTroopsSelect;
    @FXML private ComboBox<String> buildingsSelect;
    @FXML private ComboBox<String> spellsSelect;
    @FXML private ComboBox<String> winconSelect;

    private List<ComboBox<String>> sumCombos;

    // carte
    private final CardService cardService = new CardService();
    private List<Card> allCards = List.of();

    // carte possedute
    private final Set<String> ownedCardIds = new HashSet<>();

    // carte già scelte nello slot (mandatory)
    private final Set<String> selectedCardIds = new HashSet<>();

    // stato layout per pagine senza navbar
    private Node savedTop;
    private Node savedBottom;
    private Node savedCenter;

    // cache immagini
    private final Map<String, Image> imageCache = new HashMap<>();

    // guardie anti-loop
    private boolean enforcingSum = false;
    private boolean refreshingItems = false;

    @FXML
    private void initialize() {
        allCards = loadAllCardsSafe();

        var is = MainApp.class.getResourceAsStream("/img/king_expert.png");
        if (is != null && kingExpertImage != null) {
            kingExpertImage.setImage(new Image(is));
        }

        sumCombos = List.of(airTroopsSelect, buildingsSelect, spellsSelect, winconSelect);

        for (ComboBox<String> cb : sumCombos) {
            if (cb != null && cb.getValue() == null) cb.setValue("-");
        }

        setupSumConstraintWithDynamicItems();
        refreshAllComboItems();
    }

    // --------------------------
    // LOAD CARDS
    // --------------------------
    private List<Card> loadAllCardsSafe() {
        for (String p : CARDLIST_CANDIDATES) {
            if (Files.exists(Path.of(p))) {
                return cardService.loadCards(p);
            }
        }

        // fallback: se un giorno lo sposti in resources
        try (var is = MainApp.class.getResourceAsStream("/cardList.json")) {
            if (is != null) {
                Path tmp = Files.createTempFile("cardList", ".json");
                Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return cardService.loadCards(tmp.toString());
            }
        } catch (Exception ignored) {}

        System.err.println("cardList.json non trovato. Mettilo in src/main/java o in resources.");
        return List.of();
    }

    // --------------------------
    // VINCOLO: SOMMA <= 8
    // --------------------------
    private void setupSumConstraintWithDynamicItems() {
        for (ComboBox<String> cb : sumCombos) {
            if (cb == null) continue;

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

        List<String> allowed = new ArrayList<>();
        allowed.add("-");
        for (int i = 0; i <= max; i++) allowed.add(String.valueOf(i));

        if (!allowed.contains(current)) allowed.add(current);

        owner.setItems(FXCollections.observableArrayList(allowed));
        owner.setValue(current);
    }

    private int maxAllowedFor(ComboBox<String> owner) {
        int sumOthers = 0;
        for (ComboBox<String> cb : sumCombos) {
            if (cb == null || cb == owner) continue;
            sumOthers += parseValue(cb.getValue());
        }
        int remaining = MAX_SUM - sumOthers;
        return Math.max(0, Math.min(MAX_SUM, remaining));
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
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return 0; }
    }

    // --------------------------
    // TOGGLE AVANZATE
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
            c.init(allCards, ownedCardIds, this::restoreLayoutState);

            saveLayoutStateIfNeeded();
            root.setTop(null);
            root.setBottom(null);
            root.setCenter(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // BOTTONE: GA -> RISULTATO (PASSA PREFERENZE)
    // --------------------------
    @FXML
    public void GA(ActionEvent e) {
        try {
            // 1) Preferenze UI
            DeckConstraints constraints = buildConstraintsFromUI();
            double delta = (deltaSpinner != null && deltaSpinner.getValue() != null) ? deltaSpinner.getValue() : 1.0;
            double desiredAvgElixir = (avgElixirSpinner != null && avgElixirSpinner.getValue() != null) ? avgElixirSpinner.getValue() : 3.4;

            // 2) Pool carte selezionabili (owned filter)
            List<Card> pool = getSelectableCards();

            // 3) Vai alla pagina risultato
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/ResultDeckView.fxml"));
            Parent view = loader.load();

            ResultDeckController c = loader.getController();
            c.init(pool, constraints, delta, desiredAvgElixir, this::restoreLayoutState);

            saveLayoutStateIfNeeded();
            root.setTop(null);
            root.setBottom(null);
            root.setCenter(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private DeckConstraints buildConstraintsFromUI() {
        DeckConstraints c = new DeckConstraints();

        // NOTE: coerente con GA attuale (Mutation/Crossover): questi vincoli sono trattati come "minimi" (>=)
        c.nFlyingTroop = parseNullableInt(airTroopsSelect != null ? airTroopsSelect.getValue() : null);
        c.nBuildings = parseNullableInt(buildingsSelect != null ? buildingsSelect.getValue() : null);
        c.nSpells = parseNullableInt(spellsSelect != null ? spellsSelect.getValue() : null);
        c.nBuildingTarget = parseNullableInt(winconSelect != null ? winconSelect.getValue() : null);

        c.mandatoryCardsId = new ArrayList<>(selectedCardIds);
        return c;
    }

    private Integer parseNullableInt(String v) {
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty() || "-".equals(v)) return null;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return null; }
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
        if (ownedCardIds.isEmpty()) return allCards;
        return allCards.stream()
                .filter(c -> ownedCardIds.contains(c.getId()))
                .toList();
    }

    // --------------------------
    // POPUP SCELTA CARTA
    // --------------------------
    private Card showPickCardPopup(List<Card> cards) {
        Dialog<Card> dialog = new Dialog<>();
        dialog.setTitle("Scegli una carta");
        dialog.setHeaderText("Scegli una carta");

        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }

        ButtonType chooseBtn = new ButtonType("Seleziona", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(chooseBtn, ButtonType.CANCEL);

        DialogPane pane = dialog.getDialogPane();
        var cssUrl = MainApp.class.getResource("/MainView.css");
        if (cssUrl != null) pane.getStylesheets().add(cssUrl.toExternalForm());
        pane.getStyleClass().add("cr-dialog");

        Label hint = new Label("Seleziona una carta:");
        hint.getStyleClass().add("cr-dialog-hint");

        TextField search = new TextField();
        search.setPromptText("Cerca...");
        search.getStyleClass().addAll("field-input", "cr-dialog-search");

        var items = FXCollections.observableArrayList(cards);
        var filtered = new FilteredList<>(items, x -> true);

        ListView<Card> listView = new ListView<>(filtered);
        listView.getStyleClass().add("cr-dialog-list");

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

        search.textProperty().addListener((obs, o, q) -> {
            String s = (q == null) ? "" : q.toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> {
                String n = (c.getName() == null) ? "" : c.getName().toLowerCase(Locale.ROOT);
                return n.contains(s);
            });
        });

        VBox box = new VBox(10, hint, search, listView);
        box.getStyleClass().add("cr-dialog-content");
        box.setPrefSize(380, 560);
        pane.setContent(box);

        Button okBtn = (Button) pane.lookupButton(chooseBtn);
        okBtn.getStyleClass().add("cr-primary-btn");

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) cancelBtn.getStyleClass().add("cr-secondary-btn");

        okBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(bt -> bt == chooseBtn ? listView.getSelectionModel().getSelectedItem() : null);

        listView.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    // --------------------------
    // SLOT: MOSTRA IMMAGINE O NOME
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
    // IMMAGINI: prova path “per id”, altrimenti fallback alle 10 disponibili
    // --------------------------
    private Image loadCardImage(Card c) {
        if (c == null || c.getId() == null) return null;

        String id = c.getId();

        // 1) se un giorno aggiungi immagini per-id:
        String[] candidates = {
                "/img/immagini carte/" + id + ".png",
                "/img/immagini carte/" + id.replace('_', '-') + ".png",
                "/img/" + id + ".png"
        };

        for (String p : candidates) {
            var is = MainApp.class.getResourceAsStream(p);
            if (is != null) return new Image(is);
        }

        // 2) fallback alle 10
        String fallback = CARD_IMG_BY_ID.get(id);
        if (fallback == null) return null;

        return imageCache.computeIfAbsent(fallback, p -> {
            var is = MainApp.class.getResourceAsStream(p);
            return (is == null) ? null : new Image(is);
        });
    }
}
