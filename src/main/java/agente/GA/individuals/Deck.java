package agente.GA.individuals;


import model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Deck implements Comparable<Object> {
    public static final int DECK_SIZE = 8;
    private final List<Card> cards;
    private double fitness = 0.0;

    // Costruttore
    public Deck(List<Card> cards) {
        if (cards.size() != DECK_SIZE) {
            throw new IllegalArgumentException("Un deck deve avere esattamente " + DECK_SIZE + " carte.");
        }
        // Controllo duplicati (i deck di Clash Royale non hanno doppioni)
        long distinctCount = cards.stream().map(Card::getId).distinct().count();
        if (distinctCount != DECK_SIZE) {
            throw new IllegalArgumentException("Il deck non può contenere carte duplicate.");
        }

        // Creiamo una copia difensiva per evitare modifiche esterne
        this.cards = new ArrayList<>(cards);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Aggiorna le carte del deck.
     * Poiché la lista 'cards' è final, non possiamo sostituire l'oggetto,
     * ma dobbiamo svuotarlo e riempirlo con i nuovi valori.
     */
    public void setCards(List<Card> newCards) {
        // 1. Validazione base: non null e dimensione corretta
        if (newCards == null || newCards.size() != DECK_SIZE) {
            throw new IllegalArgumentException("Un deck deve contenere esattamente " + DECK_SIZE + " carte.");
        }

        // 2. Validazione Duplicati (opzionale ma consigliata)
        // In Clash Royale non puoi avere due carte uguali
        long distinctCount = newCards.stream().map(Card::getId).distinct().count();
        if (distinctCount != DECK_SIZE) {
            throw new IllegalArgumentException("Il deck non può contenere carte duplicate.");
        }

        // 3. Modifica del contenuto della lista final
        this.cards.clear();          // Svuota le vecchie carte
        this.cards.addAll(newCards); // Inserisce le nuove carte


        /**
         * Per ora lo ignoro, ma questo andrebbe rimosso perché
         * 1) non lo usiamo
         * 2) si vede che l'ha fatto chat
         *
         * */
        // 4. (Opzionale) Se hai variabili calcolate (es. costo medio elisir),
        // dovresti ricalcolarle qui.
        // this.averageElixir = calculateAverageElixir();
    }

    //TODO: DEFINIRE FITNESS @FRA
    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public String toString() {
        return "Deck [Fitness: " + String.format("%.2f", fitness) + "] Cards: " +
                cards.stream().map(Card::getName).collect(Collectors.joining(", "));
    }

    @Override
    public int compareTo(Object o) {
        return Double.compare(this.fitness, ((Deck) o).fitness);
    }
}
