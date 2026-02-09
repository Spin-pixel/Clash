package org.progettoFIA.gaclashfx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.progettoFIA.gaclashfx.storeData.UserPrefs;

public class MainViewController {
    @FXML
    public BorderPane root;

    private final UserPrefs prefs = new UserPrefs(); // condiviso


    public void openOwnedCards() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/OwnedCardsView.fxml"));
        Parent view = loader.load();


        OwnedCardsController c = loader.getController();
        c.init(prefs, () -> root.setCenter(mainContent())); // back handler

        root.setCenter(view);
    }

    public void GA() throws Exception{
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/ResultDeckView.fxml"));
        Parent view = loader.load();


        ResultDeckController c = loader.getController();

        root.setCenter(view);
    }


    private Node mainContent() {
        return new Label("Schermata principale (placeholder)");
    }
}

