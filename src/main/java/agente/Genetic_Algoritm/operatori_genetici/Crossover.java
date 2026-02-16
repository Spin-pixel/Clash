package agente.GA.operatori_genetici;

import agente.GA.individuals.*;
import model.Card.*;
import model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Crossover {

    private final List<Card> globalCardPool;
    private final Random random;

    public Crossover(List<Card> globalCardPool) {
        this.globalCardPool = new ArrayList<>(globalCardPool);
        this.random = new Random();
    }

    public List<Deck> newGeneration(List<Deck> population, int generationSize, DeckConstraints constraints) {
        List<Deck> nextGeneration = new ArrayList<>();
        List<int[]> uniquePairs = generateUniquePairs(population.size());
        Collections.shuffle(uniquePairs);

        int pairIndex = 0;
        while (nextGeneration.size() < generationSize) {

            if (pairIndex >= uniquePairs.size()) {
                pairIndex = 0;
                Collections.shuffle(uniquePairs);
            }

            int[] pair = uniquePairs.get(pairIndex++);
            Deck child = performCrossover(population.get(pair[0]), population.get(pair[1]), constraints);
            nextGeneration.add(child);
        }
        return nextGeneration;
    }

    /**
     * Esegue il crossover garantendo il rispetto dei vincoli anche in casi boundary (es. vincoli a 0).
     */
    public Deck performCrossover(Deck d1, Deck d2, DeckConstraints constraints) {
        List<Card> childCards = new ArrayList<>();
        Set<String> addedIds = new HashSet<>();

        // Pool temporaneo dai genitori mescolato
        List<Card> parentsPool = new ArrayList<>(d1.getCards());
        parentsPool.addAll(d2.getCards());
        Collections.shuffle(parentsPool, random);

        // --- FASE 1: Carte Comuni ---
        // Aggiungiamo le comuni solo se rispettano i vincoli (importante se i genitori sono sporchi)
        Set<String> d1Ids = d1.getCards().stream().map(Card::getId).collect(Collectors.toSet());
        Set<String> d2Ids = d2.getCards().stream().map(Card::getId).collect(Collectors.toSet());
        d1Ids.retainAll(d2Ids);

        for (String id : d1Ids) {
            Card card = d1.getCards().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
            if (card != null && canAdd(card, childCards, constraints)) {
                addCardToChild(childCards, addedIds, card);
            }
        }

        // --- FASE 2: Carte Obbligatorie (Mandatory) ---
        if (constraints.mandatoryCardsId != null) {
            for (String id : constraints.mandatoryCardsId) {
                if (!addedIds.contains(id)) {
                    Card card = findCardById(parentsPool, id);
                    if (card == null) card = findCardById(globalCardPool, id);

                    if (card != null) {
                        addCardToChild(childCards, addedIds, card);
                    } else {
                        throw new RuntimeException("Carta obbligatoria non trovata: " + id);
                    }
                }
            }
        }

        // --- FASE 3: Soddisfacimento Minimi ---
        // Se il vincolo è > 0, forziamo l'aggiunta di carte specifiche
        if (constraints.nSpells != null && constraints.nSpells > 0)
            ensureConstraint(childCards, addedIds, parentsPool, constraints.nSpells, c -> c.getType() == CardType.SPELL, constraints);

        if (constraints.nBuildings != null && constraints.nBuildings > 0)
            ensureConstraint(childCards, addedIds, parentsPool, constraints.nBuildings, c -> c.getType() == CardType.BUILDING, constraints);

        if (constraints.nFlyingTroop != null && constraints.nFlyingTroop > 0)
            ensureConstraint(childCards, addedIds, parentsPool, constraints.nFlyingTroop, c -> (c instanceof Troop && ((Troop) c).isFlying()), constraints);

        if (constraints.nBuildingTarget != null && constraints.nBuildingTarget > 0)
            ensureConstraint(childCards, addedIds, parentsPool, constraints.nBuildingTarget, c -> (c instanceof Troop && ((Troop) c).isTargetsOnlyBuildings()), constraints);

        // --- FASE 4: Riempimento Finale ---
        while (childCards.size() < Deck.DECK_SIZE) {
            Predicate<Card> safeToPick = c -> canAdd(c, childCards, constraints);

            // Prova dai genitori
            Card candidate = findValidCard(parentsPool, addedIds, safeToPick);
            // Fallback al pool globale
            if (candidate == null) {
                candidate = findValidCard(globalCardPool, addedIds, safeToPick);
            }

            if (candidate == null) {
                throw new RuntimeException("Impossibile completare il deck: vincoli troppo restrittivi.");
            }
            addCardToChild(childCards, addedIds, candidate);
        }

        return new Deck(childCards);
    }

    /**
     * Controlla se una carta può essere aggiunta senza superare i massimi definiti.
     * Se un vincolo è 0, questa funzione ritornerà sempre false per quel tipo di carta.
     */
    private boolean canAdd(Card c, List<Card> currentDeck, DeckConstraints k) {
        if (k.nSpells != null && c.getType() == CardType.SPELL) {
            if (countMatches(currentDeck, x -> x.getType() == CardType.SPELL) >= k.nSpells) return false;
        }
        if (k.nBuildings != null && c.getType() == CardType.BUILDING) {
            if (countMatches(currentDeck, x -> x.getType() == CardType.BUILDING) >= k.nBuildings) return false;
        }
        if (k.nFlyingTroop != null && (c instanceof Troop && ((Troop) c).isFlying())) {
            if (countMatches(currentDeck, x -> (x instanceof Troop && ((Troop) x).isFlying())) >= k.nFlyingTroop) return false;
        }
        if (k.nBuildingTarget != null && (c instanceof Troop && ((Troop) c).isTargetsOnlyBuildings())) {
            if (countMatches(currentDeck, x -> (x instanceof Troop && ((Troop) x).isTargetsOnlyBuildings())) >= k.nBuildingTarget) return false;
        }
        return true;
    }

    private void ensureConstraint(List<Card> currentDeck, Set<String> addedIds, List<Card> parentsPool, int target, Predicate<Card> condition, DeckConstraints k) {
        long currentCount = currentDeck.stream().filter(condition).count();
        while (currentCount < target) {
            // Cerchiamo una carta che soddisfi la condizione E che sia aggiungibile secondo i vincoli generali
            Card candidate = findValidCard(parentsPool, addedIds, c -> condition.test(c) && canAdd(c, currentDeck, k));

            // Fallback globale se i genitori non hanno la carta richiesta
            if (candidate == null) {
                candidate = findValidCard(globalCardPool, addedIds, c -> condition.test(c) && canAdd(c, currentDeck, k));
            }

            if (candidate != null) {
                addCardToChild(currentDeck, addedIds, candidate);
                currentCount++;
            } else {
                throw new RuntimeException("Vincolo insoddisfacibile per la categoria richiesta.");
            }
        }
    }

    private void addCardToChild(List<Card> list, Set<String> ids, Card c) {
        if (c != null && !ids.contains(c.getId())) {
            list.add(c);
            ids.add(c.getId());
        }
    }

    private Card findCardById(List<Card> source, String id) {
        return source.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private Card findValidCard(List<Card> source, Set<String> excludeIds, Predicate<Card> condition) {
        for (Card c : source) {
            if (!excludeIds.contains(c.getId()) && condition.test(c)) {
                return c;
            }
        }
        return null;
    }

    private long countMatches(List<Card> list, Predicate<Card> condition) {
        return list.stream().filter(condition).count();
    }

    private List<int[]> generateUniquePairs(int size) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                pairs.add(new int[]{i, j});
            }
        }
        return pairs;
    }
}