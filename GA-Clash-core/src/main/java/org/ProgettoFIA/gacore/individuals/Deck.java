package main.java.org.ProgettoFIA.gacore.individuals;

import main.java.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Deck {
    public static final int DECK_SIZE = 8;

    // Usiamo una Lista per comodità, ma rappresenta il tuo array di 8 geni
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
}