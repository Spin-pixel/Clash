package org.progettoFIA.gaclashfx.storeData;

/**
 * Classe per mantenere le preferenze(quali carte ha e quali no) delle utente
 * */
public class UserPrefs {
    public final javafx.collections.ObservableSet<String> ownedCardIds =
            javafx.collections.FXCollections.observableSet();
}
