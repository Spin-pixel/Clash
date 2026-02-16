package agente.GA.initializer;


import agente.GA.individuals.Deck;
import agente.GA.individuals.DeckConstraints;
import model.Card;
import model.Troop;

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
        int maxAttempts = populationSize * 3000; // Evita loop infiniti se i vincoli sono impossibili

        while (population.size() < populationSize && attempts < maxAttempts) {
            Deck candidate = generateRandomDeck(allCards, constraints);

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
     * Crea un deck di 8 carte casuali uniche (Versione per ID Stringa).
     *
     */
    private Deck generateRandomDeck(List<Card> pool, DeckConstraints constraints) {
        List<Card> selectedCards = new ArrayList<>();
        // Usiamo un Set di Stringhe per tracciare gli ID presi
        List<String> takenIds = new ArrayList<>();

        // 1. Gestione Carte Obbligatorie
        // Assumo che constraints.mandatoryCardsId sia List<String>
        if (constraints.mandatoryCardsId != null) {
            for (String mandatoryId : constraints.mandatoryCardsId) {
                // Cerchiamo la carta nel pool
                for (Card c : pool) {

                    if (c.getId().equals(mandatoryId) && !takenIds.contains(mandatoryId)) {
                        selectedCards.add(c);
                        takenIds.add(mandatoryId);
                        break; // Presa! Passiamo al prossimo ID obbligatorio
                    }
                }
            }
        }

        // 2. Mescoliamo una copia del pool per pescare le rimanenti
        List<Card> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool, random);

        // 3. Riempiamo fino a 8 carte
        for (Card candidate : shuffledPool) {
            // Stop se siamo arrivati a 8
            if (selectedCards.size() >= Deck.DECK_SIZE) {
                break;
            }

            // Aggiungiamo SOLO se l'ID non è già nel set
            String cId = candidate.getId();
            if (!takenIds.contains(cId)) {
                selectedCards.add(candidate);
                takenIds.add(cId);
            }
        }

        return new Deck(selectedCards);
    }

    /**
     * Verifica se un deck rispetta TUTTI i vincoli impostati.
     */
    private boolean isValid(Deck deck, DeckConstraints constraints) {
        List<Card> cards = deck.getCards();

        // 1. Vincolo Carte Obbligatorie (Mandatory)
        if (constraints.mandatoryCardsId != null && !constraints.mandatoryCardsId.isEmpty()) {

            // SOLUZIONE:
            // Estraiamo prima tutti gli ID dalle carte presenti nel mazzo corrente.
            // Usiamo .map(Card::getId) per trasformare la lista di oggetti Card in una lista di Stringhe.
            List<String> deckIds = cards.stream()
                    .map(Card::getId)
                    .toList(); // Usa .collect(Collectors.toList()) se sei su Java < 16

            // Ora possiamo usare containsAll perché stiamo confrontando String con String
            boolean hasAllMandatoryCards = deckIds.containsAll(constraints.mandatoryCardsId);

            // Se manca anche solo una delle carte obbligatorie, il controllo fallisce
            if (!hasAllMandatoryCards) return false;
        }

        // Contatori
        int buildingCount = 0;
        int spellCount = 0;
        int flyingTroopCount = 0;
        int buildingTargetCount = 0;


        for (Card c : cards) {
            // Conta Tipi
            if (c.getType() == Card.CardType.BUILDING) buildingCount++;
            else if (c.getType() == Card.CardType.SPELL) spellCount++;

            // Conta Air Target
            // Attenzione: Valido solo per Troop e DefensiveBuilding
            if (c instanceof Troop) {
                Troop t = (Troop) c;
                if (t.isFlying()) {
                    flyingTroopCount++;
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
        if (constraints.nBuildings != null && buildingCount != constraints.nBuildings) return false;
        if (constraints.nSpells != null && spellCount != constraints.nSpells) return false;
        if (constraints.nFlyingTroop != null && flyingTroopCount != constraints.nFlyingTroop) return false;
        if (constraints.nBuildingTarget != null && buildingTargetCount != constraints.nBuildingTarget) return false;

        return true;
    }
}
