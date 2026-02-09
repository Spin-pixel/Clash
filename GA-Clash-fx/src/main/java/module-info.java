module org.progettois.gaclashfx {
    requires javafx.controls;
    requires org.progettofia.gacore;
    requires java.desktop;
    requires javafx.fxml;
    requires java.smartcardio;
    opens org.progettoFIA.gaclashfx to javafx.fxml;



    exports org.progettoFIA.gaclashfx;
    exports org.progettoFIA.gaclashfx.temp;
    opens org.progettoFIA.gaclashfx.temp to javafx.fxml;
    exports org.progettoFIA.gaclashfx.storeData;
    opens org.progettoFIA.gaclashfx.storeData to javafx.fxml;
}
