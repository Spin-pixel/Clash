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

        // 1. Vincolo Carta Obbligatoria
        if (constraints.mandatoryCardId != null) {
            boolean hasCard = cards.stream()
                    .anyMatch(c -> c.getId().equals(constraints.mandatoryCardId));
            if (!hasCard) return false;
        }

        // Contatori
        int troopCount = 0;
        int buildingCount = 0;
        int spellCount = 0;
        int airTargetCount = 0;
        int buildingTargetCount = 0;

        for (Card c : cards) {
            // Conta Tipi
            if (c.getType() == CardType.TROOP) troopCount++;
            else if (c.getType() == CardType.BUILDING) buildingCount++;
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
        if (constraints.minTroops != null && troopCount < constraints.minTroops) return false;
        if (constraints.minBuildings != null && buildingCount < constraints.minBuildings) return false;
        if (constraints.minSpells != null && spellCount < constraints.minSpells) return false;
        if (constraints.minAirTarget != null && airTargetCount < constraints.minAirTarget) return false;
        if (constraints.minBuildingTarget != null && buildingTargetCount < constraints.minBuildingTarget) return false;

        return true;
    }
}