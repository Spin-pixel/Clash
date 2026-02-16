package agente.Genetic_Algoritm;

import agente.Genetic_Algoritm.fitness.Fitness;
import agente.Genetic_Algoritm.individuals.Deck;
import agente.Genetic_Algoritm.individuals.DeckConstraints;
import agente.Genetic_Algoritm.initializer.Initializer;
import agente.Genetic_Algoritm.operatori_genetici.Crossover;
import agente.Genetic_Algoritm.operatori_genetici.Mutation;
import agente.Genetic_Algoritm.operatori_genetici.Selection;
import model.Card;
import model.Troop;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GA_core {

    public record Params(
            int populationSize,
            int generations,
            double mutationRate,
            int genesToMutate,
            int parentsToSelect,
            int elitarism
    ) {
        public static Params defaults() {
            return new Params(
                    40,
                    100,
                    0.25,
                    2,
                    12,
                    10
            );
        }
    }

    /** Output originale: NON lo rompo */
    public record Output(Deck bestDeck, String details, String log) {}

    /** Output esteso per metriche */
    public record OutputTrace(
            Deck bestDeck,
            String details,
            String log,
            List<Double> bestFitnessPerGen,   // per convergenceGeneration()
            List<Deck> finalPopulation        // per cardUsageCV()
    ) {}

    /** Run originale: compatibile con tutto il resto */
    public static Output run(List<Card> pool,
                             DeckConstraints constraints,
                             double delta,
                             double desiredAvgElixir) {
        OutputTrace tr = runWithTrace(pool, constraints, delta, desiredAvgElixir);
        return new Output(tr.bestDeck(), tr.details(), tr.log());
    }

    /** Run con tracce per metriche */
    public static OutputTrace runWithTrace(List<Card> pool,
                                           DeckConstraints constraints,
                                           double delta,
                                           double desiredAvgElixir) {

        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(constraints, "constraints");

        StringBuilder log = new StringBuilder();
        List<Double> bestFitnessPerGen = new ArrayList<>();

        if (pool.size() < Deck.DECK_SIZE) {
            String msg = "Pool carte troppo piccolo: servono almeno 8 carte, ne hai " + pool.size();
            return new OutputTrace(null, msg, msg, List.of(), List.of());
        }

        Initializer initializer = new Initializer();
        Selection selection = new Selection();
        Crossover crossover = new Crossover(pool);   // se nel tuo progetto è new Crossover() cambia qui
        Mutation mutation = new Mutation(pool);
        Fitness fitness = new Fitness();

        List<Deck> elitarism = new ArrayList<>();

        List<Deck> population = initializer.createPopulation(pool, Params.defaults().populationSize(), constraints);

        for (Deck d : population) {
            d.setFitness(fitness.FinalFitness(d, delta, desiredAvgElixir));
        }

        Collections.sort(population);
        population = population.reversed();

        // gen 0 (inizializzazione)
        bestFitnessPerGen.add(population.getFirst().getFitness());
        log.append("Init bestFitness=")
                .append(fmt(population.getFirst().getFitness()))
                .append('\n');

        for (int round = 0, size = 0; round < Params.defaults().generations(); round++) {

            while (size < Params.defaults().elitarism()) {
                elitarism.add(population.get(size));
                size++;
            }

            List<Deck> selected = selection.select(population, Params.defaults().parentsToSelect());
            List<Deck> newGen = crossover.newGeneration(
                    selected,
                    Params.defaults().populationSize() - Params.defaults().elitarism(),
                    constraints
            );

            mutation.mutateGeneration(newGen, Params.defaults().mutationRate(), Params.defaults().genesToMutate(), constraints);

            for (Deck d : newGen) {
                d.setFitness(fitness.FinalFitness(d, delta, desiredAvgElixir));
            }

            population.clear();
            population = Stream.concat(elitarism.stream(), newGen.stream()).collect(Collectors.toList());

            Collections.sort(population);
            population = population.reversed();

            // best della generazione (round+1)
            bestFitnessPerGen.add(population.getFirst().getFitness());

            elitarism.clear();
            newGen.clear();
            size = 0;
        }

        Deck best = population.getFirst();
        String details = formatDetails(best, constraints, delta);

        return new OutputTrace(
                best,
                details,
                log.toString(),
                List.copyOf(bestFitnessPerGen),
                List.copyOf(population)
        );
    }

    private static String formatDetails(Deck best, DeckConstraints c, double delta) {
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

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
