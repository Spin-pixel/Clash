package org.progettoFIA.gaclashfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        var view = new MainView();
        Scene scene = new Scene(view.root(), 980, 640);
        //scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Mini GA + JavaFX (OneMax)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
