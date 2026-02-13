package grafica;

import agente.individuals.Deck;
import agente.individuals.DeckConstraints;
import agente.initializer.Initializer;
import agente.operatori_genetici.Crossover;
import agente.operatori_genetici.Mutation;
import agente.operatori_genetici.Selection;
import model.Card;
import model.Troop;

import java.util.*;

public final class ResultDeck {

    private ResultDeck() {}

    public record Params(
            int populationSize,
            int generations,
            double mutationRate,
            int genesToMutate,
            int parentsToSelect
    ) {
        public static Params defaults() {
            return new Params(
                    40,   // population
                    35,   // generations
                    0.25, // mutationRate
                    2,    // genesToMutate
                    12    // parentsToSelect (>=10 per evitare il limite di coppie uniche nel crossover)
            );
        }
    }

    public record Output(Deck bestDeck, String details, String log) {}

    public static Output run(List<Card> pool,
                             DeckConstraints constraints,
                             double delta,
                             double desiredAvgElixir,
                             Params params) {

        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(params, "params");

        StringBuilder log = new StringBuilder();

        if (pool.size() < Deck.DECK_SIZE) {
            String msg = "Pool carte troppo piccolo: servono almeno 8 carte, ne hai " + pool.size();
            return new Output(null, msg, msg);
        }

        // sanitizza mandatory (no null, no vuoti, max 8, no duplicati)
        constraints.mandatoryCardsId = sanitizeMandatory(constraints.mandatoryCardsId);

        Initializer initializer = new Initializer();
        Selection selection = new Selection();
        Crossover crossover = new Crossover();
        Mutation mutation = new Mutation(pool);

        Crossover.setLogging(false);

        List<Deck> population = initializer.createPopulation(pool, params.populationSize, constraints);
        if (population.isEmpty()) {
            String msg = "Impossibile generare la popolazione iniziale con i vincoli dati (prova ad allentare i vincoli o scegliere più carte possedute).";
            return new Output(null, msg, msg);
        }

        evaluatePopulation(population, constraints, delta, desiredAvgElixir);

        Deck best = bestOf(population);
        log.append("Init bestFitness=").append(fmt(best.getFitness())).append("\n");

        for (int gen = 1; gen <= params.generations; gen++) {

            int nParents = Math.min(params.parentsToSelect, population.size());
            if (nParents < 2) nParents = Math.min(2, population.size());

            List<Deck> parents = selection.select(population, nParents);

            List<Deck> children = crossover.newGeneration(parents, params.populationSize, constraints);

            // se il crossover non riesce a generare abbastanza figli (limite coppie uniche), riempio
            if (children.size() < params.populationSize) {
                int missing = params.populationSize - children.size();
                children.addAll(initializer.createPopulation(pool, missing, constraints));
            }
            while (children.size() < params.populationSize) {
                // ultimissimo fallback: deck random (potrebbe non rispettare i vincoli, ma la fitness lo penalizza)
                children.add(randomDeck(pool, constraints.mandatoryCardsId));
            }

            mutation.mutateGeneration(children, params.mutationRate, params.genesToMutate, constraints);

            evaluatePopulation(children, constraints, delta, desiredAvgElixir);

            Deck bestChild = bestOf(children);
            if (bestChild.getFitness() > best.getFitness()) best = bestChild;

            log.append("Gen ").append(gen)
                    .append(": bestChild=").append(fmt(bestChild.getFitness()))
                    .append(" | bestGlobal=").append(fmt(best.getFitness()))
                    .append("\n");

            population = children;
        }

        String details = formatDetails(best, constraints, delta, desiredAvgElixir);
        return new Output(best, details, log.toString());
    }

    // ----------------------
    // FITNESS
    // ----------------------

    private static void evaluatePopulation(List<Deck> pop,
                                           DeckConstraints constraints,
                                           double delta,
                                           double desiredAvgElixir) {
        for (Deck d : pop) {
            d.setFitness(fitness(d, constraints, delta, desiredAvgElixir));
        }
    }

    private static double fitness(Deck deck,
                                  DeckConstraints constraints,
                                  double delta,
                                  double desiredAvgElixir) {

        if (deck == null) return 0.0;
        List<Card> cards = deck.getCards();
        if (cards == null || cards.size() != Deck.DECK_SIZE) return 0.0;

        // vincoli (coerenti con Mutation/Crossover: MINIMI >=)
        if (!respectsConstraintsMin(cards, constraints)) return 0.0;

        double avgElixir = averageElixir(cards);
        double elixirComponent = clamp01(1.0 - (Math.abs(avgElixir - desiredAvgElixir) / 3.0));

        double effComponent = efficiencyComponent(cards);

        // delta: peso preferenza vs efficienza
        double w = clamp01(delta);
        return clamp01(w * elixirComponent + (1.0 - w) * effComponent);
    }

    private static double efficiencyComponent(List<Card> cards) {
        double sum = 0.0;
        for (Card c : cards) {
            sum += Math.log1p(Math.max(0.0, c.getEfficiencyScore()));
        }
        double avg = sum / cards.size();
        // normalizzazione grezza
        return clamp01(avg / 10.0);
    }

    // ----------------------
    // VINCOLI: MINIMI (>=)
    // ----------------------

    private static boolean respectsConstraintsMin(List<Card> cards, DeckConstraints c) {
        if (c == null) return true;

        if (c.mandatoryCardsId != null && !c.mandatoryCardsId.isEmpty()) {
            Set<String> ids = new HashSet<>();
            for (Card card : cards) ids.add(card.getId());
            if (!ids.containsAll(c.mandatoryCardsId)) return false;
        }

        int spells = 0;
        int buildings = 0;
        int flying = 0;
        int wincon = 0;

        for (Card card : cards) {
            if (card.getType() == Card.CardType.SPELL) spells++;
            if (card.getType() == Card.CardType.BUILDING) buildings++;
            if (card instanceof Troop t) {
                if (t.isFlying()) flying++;
                if (t.isTargetsOnlyBuildings()) wincon++;
            }
        }

        if (c.nSpells != null && spells < c.nSpells) return false;
        if (c.nBuildings != null && buildings < c.nBuildings) return false;
        if (c.nFlyingTroop != null && flying < c.nFlyingTroop) return false;
        if (c.nBuildingTarget != null && wincon < c.nBuildingTarget) return false;

        return true;
    }

    // ----------------------
    // DETAILS / UTILS
    // ----------------------

    private static String formatDetails(Deck best,
                                        DeckConstraints c,
                                        double delta,
                                        double desiredAvgElixir) {
        if (best == null) return "Nessun deck generato.";

        List<Card> cards = best.getCards();

        int spells = 0, buildings = 0, flying = 0, wincon = 0;
        for (Card card : cards) {
            if (card.getType() == Card.CardType.SPELL) spells++;
            if (card.getType() == Card.CardType.BUILDING) buildings++;
            if (card instanceof Troop t) {
                if (t.isFlying()) flying++;
                if (t.isTargetsOnlyBuildings()) wincon++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Fitness: ").append(fmt(best.getFitness())).append("\n");
        sb.append("Delta: ").append(fmt(delta)).append("\n");
        sb.append("Media elisir: ").append(fmt(averageElixir(cards)))
                .append(" (target ").append(fmt(desiredAvgElixir)).append(")\n\n");

        sb.append("Vincoli (minimi):\n");
        sb.append("- Air >= ").append(c.nFlyingTroop).append("\n");
        sb.append("- Buildings >= ").append(c.nBuildings).append("\n");
        sb.append("- Spells >= ").append(c.nSpells).append("\n");
        sb.append("- Wincon >= ").append(c.nBuildingTarget).append("\n\n");

        sb.append("Conteggi deck:\n");
        sb.append("- Air = ").append(flying).append("\n");
        sb.append("- Buildings = ").append(buildings).append("\n");
        sb.append("- Spells = ").append(spells).append("\n");
        sb.append("- Wincon = ").append(wincon).append("\n\n");

        if (c.mandatoryCardsId != null && !c.mandatoryCardsId.isEmpty()) {
            sb.append("Carte obbligatorie: ").append(c.mandatoryCardsId).append("\n\n");
        }

        sb.append("Carte:\n");
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            sb.append(i + 1).append(") ").append(card.getName())
                    .append(" (").append(card.getId()).append(")\n");
        }

        return sb.toString();
    }

    private static Deck bestOf(List<Deck> pop) {
        Deck best = pop.getFirst();
        for (Deck d : pop) if (d.getFitness() > best.getFitness()) best = d;
        return best;
    }

    private static double averageElixir(List<Card> cards) {
        double sum = 0.0;
        for (Card c : cards) sum += c.getElixirCost();
        return sum / cards.size();
    }

    private static List<String> sanitizeMandatory(List<String> ids) {
        if (ids == null) return new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null) continue;
            String s = id.trim();
            if (!s.isEmpty()) set.add(s);
            if (set.size() >= Deck.DECK_SIZE) break;
        }
        return new ArrayList<>(set);
    }

    private static Deck randomDeck(List<Card> pool, List<String> mandatoryIds) {
        Random r = new Random();
        List<Card> cards = new ArrayList<>();
        Set<String> taken = new HashSet<>();

        if (mandatoryIds != null) {
            for (String id : mandatoryIds) {
                for (Card c : pool) {
                    if (Objects.equals(c.getId(), id) && taken.add(id)) {
                        cards.add(c);
                        break;
                    }
                }
            }
        }

        List<Card> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, r);

        for (Card c : shuffled) {
            if (cards.size() >= Deck.DECK_SIZE) break;
            if (taken.add(c.getId())) cards.add(c);
        }

        while (cards.size() < Deck.DECK_SIZE) {
            Card c = pool.get(r.nextInt(pool.size()));
            if (taken.add(c.getId())) cards.add(c);
        }

        return new Deck(cards);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
