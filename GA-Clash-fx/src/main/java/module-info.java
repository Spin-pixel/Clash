module org.progettois.gaclashfx {
    requires javafx.controls;
    requires org.progettofia.gacore;
    requires java.desktop;
    requires javafx.fxml;
    opens org.progettoFIA.gaclashfx to javafx.fxml;

    exports org.progettoFIA.gaclashfx;
}
