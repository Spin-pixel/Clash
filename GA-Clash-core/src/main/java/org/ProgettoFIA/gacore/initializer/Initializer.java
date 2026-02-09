package main.java.org.ProgettoFIA.gacore.initializer;

import main.java.model.*;
import main.java.model.Card.*;

import main.java.org.ProgettoFIA.gacore.individuals.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Initializer {

    private final Random random = new Random();

    /**
     * Genera una popolazione iniziale di dimensione specificata rispettando i vincoli.
     */
    public List<Deck> createPopulation(List<Card> allCards, int populationSize, DeckConstraints constraints) {
        List<Deck> population = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = populationSize * 1000; // Evita loop infiniti se i vincoli sono impossibili

        while (population.size() < populationSize && attempts < maxAttempts) {
            Deck candidate = generateRandomDeck(allCards);

            if (isValid(candidate, constraints)) {
                population.add(candidate);
            }
            attempts++;
        }

        if (population.size() < populationSize) {
            System.err.println("ATTENZIONE: Non sono riuscito a generare l'intera popolazione richiesta rispettando i vincoli stringenti.");
        }

        return population;
    }

    /**
     * Crea un deck di 8 carte casuali uniche.
     */
    private Deck generateRandomDeck(List<Card> pool) {
        List<Card> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool, random);

        // Prende le prime 8 carte
        List<Card> selectedCards = new ArrayList<>();
        for (int i = 0; i < Deck.DECK_SIZE; i++) {
            selectedCards.add(shuffledPool.get(i));
        }

        return new Deck(selectedCards);
    }

    /**
     * Verifica se un deck rispetta TUTTI i vincoli impostati.
     */
    private boolean isValid(Deck deck, DeckConstraints constraints) {
        List<Card> cards = deck.getCards();

        // 1. Vincolo Carte Escluse (Blacklist)
        if (constraints.mandatoryCardsId != null && !constraints.mandatoryCardsId.isEmpty()) {

            // Controlla se una qualsiasi (anyMatch) delle carte attuali è contenuta nella lista di quelle escluse
            boolean hasForbiddenCard = cards.stream()
                    .anyMatch(c -> constraints.mandatoryCardsId.contains(c));

            // Se ne ha trovata almeno una vietata, il controllo fallisce
            if (hasForbiddenCard) return false;
        }

        // Contatori
        int buildingCount = 0;
        int spellCount = 0;
        int airTargetCount = 0;
        int buildingTargetCount = 0;


        //TODO: Post modifica classi, adatta i metodi
        for (Card c : cards) {
            // Conta Tipi
            if (c.getType() == CardType.BUILDING) buildingCount++;
            else if (c.getType() == CardType.SPELL) spellCount++;

            // Conta Air Target
            // Attenzione: Valido solo per Troop e DefensiveBuilding
            if (c instanceof Troop) {
                Troop t = (Troop) c;
                if (t.getAttackScope() == AttackScope.AIR_GROUND) {
                    airTargetCount++;
                }
            }
            // Oppure controlliamo se è un Edificio Difensivo
            else if (c instanceof DefensiveBuilding) {
                DefensiveBuilding db = (DefensiveBuilding) c;
                if (db.getAttackScope() == AttackScope.AIR_GROUND) {
                    airTargetCount++;
                }
            }

            // Conta Building Target (Solo Troops hanno questo campo specifico nel tuo modello)
            if (c instanceof Troop) {
                if (((Troop) c).isTargetsOnlyBuildings()) {
                    buildingTargetCount++;
                }
            }
        }

        // Verifica conteggi
        if (constraints.nBuildings != null && buildingCount < constraints.nBuildings) return false;
        if (constraints.nSpells != null && spellCount < constraints.nSpells) return false;
//        if (constraints. != null && airTargetCount < constraints.nAirTarget) return false;
        if (constraints.nBuildingTarget != null && buildingTargetCount < constraints.nBuildingTarget) return false;

        return true;
    }
}