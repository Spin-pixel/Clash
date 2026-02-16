module org.progettofia.royalgenerator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires java.prefs;

    requires com.fasterxml.jackson.databind;

    // JavaFX FXML usa reflection sui controller
    opens grafica to javafx.fxml;

    // Se in futuro userai oggetti di questi package dentro FXML (custom components), meglio aprirli
    opens model to javafx.fxml;


    // Export (utile per launcher / plugin / altri moduli)
    exports grafica;
    exports model;
    exports service;
    exports metriche;
    exports agente.Genetic_Algoritm;
    exports agente.Simulated_Annealing;

    // (facoltativi) esporta anche il core GA se lo vuoi usare da fuori modulo
    exports agente.Genetic_Algoritm.individuals;
    exports agente.Genetic_Algoritm.initializer;
    exports agente.Genetic_Algoritm.operatori_genetici;
    exports grafica.storeData;
    opens grafica.storeData to javafx.fxml;
}