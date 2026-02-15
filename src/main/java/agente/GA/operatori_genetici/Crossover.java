package agente.GA.operatori_genetici;


import agente.GA.individuals.Deck;
import agente.GA.individuals.DeckConstraints;
import model.Card;
import model.SpawnerTroop;
import model.Troop;

import java.util.*;


import java.util.logging.Logger;

public class Crossover {

    private static final Logger LOGGER = Logger.getLogger(Crossover.class.getName());
    private final Random random = new Random();

    /**
     * Genera una nuova popolazione di deck incrociando coppie uniche di genitori.
     */
    public List<Deck> newGeneration(List<Deck> population, int generationSize, DeckConstraints constraints) {
        List<Deck> nextGeneration = new ArrayList<>();
        List<int[]> uniquePairs = generateUniquePairs(population.size());

        int pairIndex = 0;
        while (nextGeneration.size() < generationSize) {
            if (pairIndex >= uniquePairs.size()) {
                LOGGER.warning("Combinazioni uniche esaurite. Generati " + nextGeneration.size() + "/" + generationSize);
                break;
            }

            int[] pair = uniquePairs.get(pairIndex++);
            Deck child = performCrossover(population.get(pair[0]), population.get(pair[1]), constraints);
            nextGeneration.add(child);
        }

        return nextGeneration;
    }

    /**
     * Logica principale del Crossover.
     */
    private Deck performCrossover(Deck parent1, Deck parent2, DeckConstraints constraints) {
        Set<Card> childCards = new HashSet<>();

        // --- 1. EREDITARIETÀ CARTE COMUNI ---
        Set<Card> commonCards = new HashSet<>(parent1.getCards());
        commonCards.retainAll(parent2.getCards());
        childCards.addAll(commonCards);

        // --- 2. CLASSIFICAZIONE (BUCKETS) ---
        // Distribuisce le carte NON comuni nei bucket specifici per soddisfare i vincoli
        Map<String, List<Card>> p1Buckets = classifyCards(parent1, commonCards);
        Map<String, List<Card>> p2Buckets = classifyCards(parent2, commonCards);

        // --- 3. SODDISFACIMENTO VINCOLI ---
        // Preleva dai bucket specifici finché i requisiti non sono soddisfatti
        fillConstraint(childCards, p1Buckets, p2Buckets, "BUILDINGS", constraints.nBuildings);
        fillConstraint(childCards, p1Buckets, p2Buckets, "SPELLS", constraints.nSpells);
        fillConstraint(childCards, p1Buckets, p2Buckets, "FLYING", constraints.nFlyingTroop);
        fillConstraint(childCards, p1Buckets, p2Buckets, "TARGET_TOWER", constraints.nBuildingTarget);

        // --- 4. CONSOLIDAMENTO SCARTI (OPZIONE A - FIX) ---
        // Uniamo tutto ciò che è rimasto nei vari bucket in un unico pool per il riempimento finale
        List<Card> p1Leftovers = consolidateLeftovers(p1Buckets);
        List<Card> p2Leftovers = consolidateLeftovers(p2Buckets);

        // --- 5. FILLER (RIEMPIMENTO FINALE) ---
        int slotsNeeded = 8 - childCards.size();

        for (int i = 0; i < slotsNeeded; i++) {
            Card chosen = pickRandomCard(p1Leftovers, p2Leftovers);
            if (chosen != null) {
                childCards.add(chosen);
            } else {
                // Questo accade solo se i genitori hanno meno di 8 carte totali uniche combinate (improbabile)
                LOGGER.warning("Impossibile riempire il mazzo: genitori a secco!");
            }
        }

        return new Deck(new ArrayList<>(childCards));
    }

    // -------------------------------------------------------------------------
    //                              HELPER METHODS
    // -------------------------------------------------------------------------

    /**
     * Tenta di soddisfare un vincolo specifico prelevando dai bucket corrispondenti.
     */
    private void fillConstraint(Set<Card> childCards,
                                Map<String, List<Card>> p1Buckets,
                                Map<String, List<Card>> p2Buckets,
                                String category,
                                Integer required) {
        if (required == null || required <= 0) return;

        // Conta quante ne abbiamo già (ereditate dalle comuni)
        long currentCount = childCards.stream().filter(c -> isType(c, category)).count();
        int needed = required - (int) currentCount;

        List<Card> p1List = p1Buckets.get(category);
        List<Card> p2List = p2Buckets.get(category);

        for (int i = 0; i < needed; i++) {
            Card picked = pickRandomCard(p1List, p2List);
            if (picked != null) {
                childCards.add(picked);
            }
        }
    }

    /**
     * (OPZIONE A) Raccoglie tutte le carte rimaste in tutti i bucket in un'unica lista.
     */
    private List<Card> consolidateLeftovers(Map<String, List<Card>> buckets) {
        List<Card> pool = new ArrayList<>();
        // Aggiunge il contenuto di TUTTE le liste (Buildings, Spells, Others, ecc.)
        buckets.values().forEach(pool::addAll);
        Collections.shuffle(pool); // Mischia per garantire casualità nel filler
        return pool;
    }

    /**
     * Seleziona una carta a caso tra due liste. Rimuove la carta selezionata dalla lista d'origine.
     */
    private Card pickRandomCard(List<Card> list1, List<Card> list2) {
        if (list1.isEmpty() && list2.isEmpty()) return null;

        boolean pickFrom1;
        if (!list1.isEmpty() && !list2.isEmpty()) {
            pickFrom1 = random.nextBoolean();
        } else {
            pickFrom1 = !list1.isEmpty();
        }

        return pickFrom1 ? list1.remove(0) : list2.remove(0);
    }

    /**
     * Classifica le carte di un genitore nei bucket.
     * Ordine di priorità: Building > Spell > Flying > Target Tower > Others.
     */
    private Map<String, List<Card>> classifyCards(Deck deck, Set<Card> excludeCards) {
        Map<String, List<Card>> buckets = new HashMap<>();
        buckets.put("BUILDINGS", new ArrayList<>());
        buckets.put("SPELLS", new ArrayList<>());
        buckets.put("FLYING", new ArrayList<>());
        buckets.put("TARGET_TOWER", new ArrayList<>());
        buckets.put("OTHERS", new ArrayList<>());

        for (Card c : deck.getCards()) {
            if (excludeCards.contains(c)) continue;

            if (isType(c, "BUILDINGS")) {
                buckets.get("BUILDINGS").add(c);
            } else if (isType(c, "SPELLS")) {
                buckets.get("SPELLS").add(c);
            } else if (isType(c, "FLYING")) {
                buckets.get("FLYING").add(c);
            } else if (isType(c, "TARGET_TOWER")) {
                buckets.get("TARGET_TOWER").add(c);
            } else {
                buckets.get("OTHERS").add(c);
            }
        }
        // Mischiamo subito i bucket per non prendere sempre le carte nello stesso ordine
        buckets.values().forEach(Collections::shuffle);
        return buckets;
    }

    /**
     * Controllo centralizzato e sicuro sui tipi di carte.
     */
    private boolean isType(Card c, String category) {
        switch (category) {
            case "BUILDINGS":
                return c.getType() == Card.CardType.BUILDING;
            case "SPELLS":
                return c.getType() == Card.CardType.SPELL;
            case "FLYING":
                // Controlla Troop e SpawnerTroop
                if (c instanceof Troop) return ((Troop) c).isFlying();
                if (c instanceof SpawnerTroop) return ((SpawnerTroop) c).isFlying();
                return false;
            case "TARGET_TOWER":
                if (c instanceof Troop) return ((Troop) c).isTargetsOnlyBuildings();
                if (c instanceof SpawnerTroop) return ((SpawnerTroop) c).isTargetsOnlyBuildings();
                // Nota: Building che targettano building (XBow/Mortar) non sono "TargetOnlyBuilding" boolean property, ma Building type.
                // Se nel tuo JSON XBow ha targetsOnlyBuildings=true, aggiungi il check qui.
                return false;
            default:
                return false;
        }
    }

    private List<int[]> generateUniquePairs(int size) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                pairs.add(new int[]{i, j});
            }
        }
        Collections.shuffle(pairs);
        return pairs;
    }

    // Configurazione Logger (Invariata)
    public static void setLogging(boolean active) {
        LOGGER.setLevel(active ? java.util.logging.Level.INFO : java.util.logging.Level.OFF);
    }

    static {
        LOGGER.setUseParentHandlers(false);
        java.util.logging.Handler handler = new java.util.logging.StreamHandler(System.out, new java.util.logging.SimpleFormatter());
        handler.setLevel(java.util.logging.Level.INFO);
        LOGGER.addHandler(handler);
    }
}