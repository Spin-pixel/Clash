package main.java.org.ProgettoFIA.gacore.operatori_genetici;

import main.java.model.*;
import main.java.model.Card.*;
import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Crossover {

    // 1. Definiamo il Logger
    private static final Logger LOGGER = Logger.getLogger(Crossover.class.getName());
    private final Random random = new Random();

    public Deck performCrossover(Deck parent1, Deck parent2, DeckConstraints constraints) {
        // Log Iniziale
        LOGGER.info(">>> INIZIO CROSSOVER <<<");
        LOGGER.fine("Parent 1: " + getIds(parent1.getCards()));
        LOGGER.fine("Parent 2: " + getIds(parent2.getCards()));

        Set<Card> childCards = new HashSet<>();

        // --- FASE 1: INTERSEZIONE ---
        Set<Card> commonCards = new HashSet<>(parent1.getCards());
        commonCards.retainAll(parent2.getCards());
        childCards.addAll(commonCards);

        LOGGER.info("[1] Carte Comuni (ereditate automaticamente): " + getIds(commonCards));

        // --- FASE 2: CLASSIFICAZIONE (BUCKETS) ---
        Map<String, List<Card>> p1Buckets = classifyCards(parent1, commonCards, constraints);
        Map<String, List<Card>> p2Buckets = classifyCards(parent2, commonCards, constraints);

        // Logghiamo cosa c'è nei bucket per capire le disponibilità
        logBucketAvailability("P1", p1Buckets);
        logBucketAvailability("P2", p2Buckets);

        // --- FASE 3: VINCOLI SPECIFICI ---
        LOGGER.info("[2] Gestione Vincoli:");
        handleConstraintCategory(childCards, p1Buckets, p2Buckets, "BUILDINGS", constraints.nBuildings);
        handleConstraintCategory(childCards, p1Buckets, p2Buckets, "SPELLS", constraints.nSpells);
        handleConstraintCategory(childCards, p1Buckets, p2Buckets, "FLYING", constraints.nFlyingTroop);
        handleConstraintCategory(childCards, p1Buckets, p2Buckets, "TARGET_TOWER", constraints.nBuildingTarget);

        // --- FASE 4: FILLER ---
        int remainingSlots = 8 - childCards.size();
        LOGGER.info("[3] Riempimento Filler (" + remainingSlots + " slot rimasti):");

        List<Card> p1Rest = p1Buckets.get("OTHERS");
        List<Card> p2Rest = p2Buckets.get("OTHERS");

        for (int i = 0; i < remainingSlots; i++) {
            Card selected = null;
            String source = "";

            if (random.nextBoolean() && !p1Rest.isEmpty()) {
                selected = p1Rest.removeFirst();
                source = "P1";
            } else if (!p2Rest.isEmpty()) {
                selected = p2Rest.removeFirst();
                source = "P2";
            } else if (!p1Rest.isEmpty()) {
                selected = p1Rest.removeFirst();
                source = "P1 (Fallback)";
            }

            if (selected != null) {
                childCards.add(selected);
                LOGGER.info("   -> Filler aggiunto: " + selected.getId() + " da " + source);
            } else {
                LOGGER.warning("   -> Impossibile trovare carta filler! I genitori sono vuoti?");
            }
        }

        Deck child = new Deck(new ArrayList<>(childCards));
        LOGGER.info(">>> FINE CROSSOVER. Figlio generato: " + getIds(child.getCards()) + " (Size: " + child.getCards().size() + ")");
        LOGGER.info("--------------------------------------------------");

        return child;
    }

    private void handleConstraintCategory(Set<Card> childCards,
                                          Map<String, List<Card>> p1Buckets,
                                          Map<String, List<Card>> p2Buckets,
                                          String bucketKey,
                                          Integer requiredTotal) {
        if (requiredTotal == null || requiredTotal == 0) return;

        long alreadyPresent = childCards.stream().filter(c -> checkCategory(c, bucketKey)).count();
        int slotsToFill = requiredTotal - (int)alreadyPresent;

        LOGGER.info("   Cat: " + bucketKey + " | Richiesti: " + requiredTotal +
                " | Presenti: " + alreadyPresent + " | Da riempire: " + slotsToFill);

        List<Card> p1Candidates = p1Buckets.getOrDefault(bucketKey, new ArrayList<>());
        List<Card> p2Candidates = p2Buckets.getOrDefault(bucketKey, new ArrayList<>());

        for (int i = 0; i < slotsToFill; i++) {
            Card chosen = null;
            String parent = "";

            if (random.nextBoolean()) {
                if (!p1Candidates.isEmpty()) { chosen = p1Candidates.remove(0); parent = "P1"; }
                else if (!p2Candidates.isEmpty()) { chosen = p2Candidates.remove(0); parent = "P2"; }
            } else {
                if (!p2Candidates.isEmpty()) { chosen = p2Candidates.remove(0); parent = "P2"; }
                else if (!p1Candidates.isEmpty()) { chosen = p1Candidates.remove(0); parent = "P1"; }
            }

            if (chosen != null) {
                childCards.add(chosen);
                LOGGER.info("      + Aggiunto " + chosen.getId() + " da " + parent);
            } else {
                LOGGER.warning("      ! Impossibile soddisfare vincolo " + bucketKey + ": genitori a secco.");
            }
        }
    }

    // --- METODI HELPER E LOGGING ---

    // Stampa ID delle carte per leggibilità (es. [Zap, Hog, Cannon])
    private String getIds(Collection<Card> cards) {
        return "[" + cards.stream().map(Card::getId).collect(Collectors.joining(", ")) + "]";
    }

    // Logga il contenuto dei bucket
    private void logBucketAvailability(String parentName, Map<String, List<Card>> buckets) {
        StringBuilder sb = new StringBuilder(parentName + " Disponibilità: ");
        buckets.forEach((k, v) -> {
            if(!v.isEmpty()) sb.append(k).append("=").append(v.size()).append(" ");
        });
        LOGGER.fine(sb.toString());
    }

    // Include la tua fix per il ClassCastException
    private boolean checkCategory(Card c, String key) {
        if (key.equals("BUILDINGS")) return c.getType() == CardType.BUILDING;
        if (key.equals("SPELLS")) return c.getType() == CardType.SPELL;
        if (key.equals("FLYING")) return (c instanceof Troop) && ((Troop)c).isFlying();
        if (key.equals("TARGET_TOWER")) return (c instanceof Troop) && ((Troop)c).isTargetsOnlyBuildings();
        return false;
    }

    // classifyCards rimane uguale alla tua versione precedente...
    private Map<String, List<Card>> classifyCards(Deck deck, Set<Card> commonCards, DeckConstraints constraints) {
        Map<String, List<Card>> buckets = new HashMap<>();
        buckets.put("BUILDINGS", new ArrayList<>());
        buckets.put("SPELLS", new ArrayList<>());
        buckets.put("FLYING", new ArrayList<>());
        buckets.put("TARGET_TOWER", new ArrayList<>());
        buckets.put("OTHERS", new ArrayList<>());

        for (Card c : deck.getCards()) {
            if (commonCards.contains(c)) continue;

            if (c.getType() == CardType.BUILDING) {
                buckets.get("BUILDINGS").add(c);
            } else if (c.getType() == CardType.SPELL) {
                buckets.get("SPELLS").add(c);
            } else if (c.getType() == CardType.TROOP && ((Troop)c).isFlying()) {
                buckets.get("FLYING").add(c);
            } else if (c.getType() == CardType.TROOP && ((Troop)c).isTargetsOnlyBuildings()) {
                buckets.get("TARGET_TOWER").add(c);
            } else {
                buckets.get("OTHERS").add(c);
            }
        }
        buckets.values().forEach(Collections::shuffle);
        return buckets;
    }

    static {
        LOGGER.setUseParentHandlers(false);

        java.util.logging.Handler handler = new java.util.logging.StreamHandler(System.out, new java.util.logging.Formatter() {
            @Override
            public String format(java.util.logging.LogRecord record) {
                return record.getMessage() + "\n";
            }
        }) {
            // Forza flush() a ogni messaggio per vederlo in tempo reale.
            @Override
            public synchronized void publish(java.util.logging.LogRecord record) {
                super.publish(record);
                flush();
            }
        };

        // Imposta il livello di logging (INFO mostra tutto quello che hai scritto)
        handler.setLevel(java.util.logging.Level.INFO);
        LOGGER.addHandler(handler);
    }

    public static void setLogging(boolean active) {
        if (active) {
            LOGGER.setLevel(java.util.logging.Level.INFO);
            System.out.println("--- LOGGING CROSSOVER ATTIVATO ---");
        } else {
            LOGGER.setLevel(java.util.logging.Level.OFF);
            // Non stampiamo nulla quando disattiviamo
        }
    }

}