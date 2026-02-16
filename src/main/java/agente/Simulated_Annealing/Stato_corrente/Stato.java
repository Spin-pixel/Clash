package agente.Simulated_Annealing.Stato_corrente;


import model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Posso definire lo stato di un individuo in modo analogo a come ho definito il deck per il GA
 * @see agente.Genetic_Algoritm.individuals.Deck
 *
 * Quindi in modo analogo posso definire i vincoli in
 * @see Vincoli
 * */
public class Stato implements Comparable<Stato> {
    public static final int SIZE_STATO = 8;
    private final List<Card> cards;
    private double utility = 0.0;

    public Stato(List<Card> cards) {
        //Controllo sulla taglia del deck
        if(cards.size() != SIZE_STATO)
            throw new IllegalArgumentException("Cards size must be " + SIZE_STATO + " but was " + cards.size());
        // Controllo duplicati (i deck di Clash Royale non hanno doppioni)
        long distinctCount = cards.stream().map(Card::getId).distinct().count();
        if (distinctCount != SIZE_STATO) {
            throw new IllegalArgumentException("Il deck non può contenere carte duplicate.");
        }
        // Creiamo una copia final per evitare modifiche esterne
        this.cards = new ArrayList<>(cards);
    }


    public List<Card> getCards() {
        return  Collections.unmodifiableList(cards);
    }

    /**
     * Rieffettuo i controlli sullo stato per verificare non violi nessun vincolo
     * Dato che è una variabile di tipo final per settarla devo pulire la lista e poi posso ri-inizializzarla
     * */
    public void setCards(List<Card> newCards) {
        // 1. Validazione base: non null e dimensione corretta
        if (newCards == null || newCards.size() != SIZE_STATO) {
            throw new IllegalArgumentException("Un deck deve contenere esattamente " + SIZE_STATO + " carte.");
        }

        // 2. Validazione Duplicati
        // In Clash Royale non puoi avere due carte uguali
        long distinctCount = newCards.stream().map(Card::getId).distinct().count();
        if (distinctCount != SIZE_STATO) {
            throw new IllegalArgumentException("Il deck non può contenere carte duplicate.");
        }

        this.cards.clear();
        this.cards.addAll(newCards);
    }

    public double getUtility() {
        return utility;
    }
    public void setUtility(double newUtility) {
        if (newUtility != utility) {
            utility = newUtility;
        }
    }

    @Override
    public String toString() {
        return "Stato [Utilità: " + String.format("%.2f", utility) + "] Cards: " +
                cards.stream().map(Card::getName).collect(Collectors.joining(", "));
    }

    @Override
    public int compareTo(Stato s) {
        return Double.compare(this.utility, s.getUtility());
    }
}
