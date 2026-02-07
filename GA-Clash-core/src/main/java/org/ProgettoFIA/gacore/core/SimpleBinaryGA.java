package org.ProgettoFIA.gacore.core;



import org.ProgettoFIA.gacore.api.GenerationListener;
import org.ProgettoFIA.gacore.api.GenerationStats;
import org.ProgettoFIA.gacore.api.StopToken;
import org.ProgettoFIA.gacore.fitness.OneMaxFitness;
import org.ProgettoFIA.gacore.individuals.BinaryIndividual;
import org.ProgettoFIA.gacore.operatori_genetici.BitFlipMutation;
import org.ProgettoFIA.gacore.operatori_genetici.SinglePointCrossover;
import org.ProgettoFIA.gacore.operatori_genetici.TournamentSelection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class SimpleBinaryGA {

    private final OneMaxFitness fitness = new OneMaxFitness();
    private final TournamentSelection selection = new TournamentSelection();
    private final SinglePointCrossover crossover = new SinglePointCrossover();
    private final BitFlipMutation mutation = new BitFlipMutation();

    public BinaryIndividual run(GAConfig cfg, StopToken stopToken, GenerationListener listener) {
        Random rnd = new Random(cfg.randomSeed());

        // 1) Init population
        List<BinaryIndividual> pop = new ArrayList<>(cfg.populationSize());
        for (int i = 0; i < cfg.populationSize(); i++) {
            BinaryIndividual ind = BinaryIndividual.random(cfg.chromosomeLength(), rnd);
            ind.setFitness(fitness.evaluate(ind));
            pop.add(ind);
        }

        BinaryIndividual globalBest = bestOf(pop).clone();

        // 2) Evolve
        for (int gen = 0; gen < cfg.maxGenerations(); gen++) {
            if (stopToken != null && stopToken.isStopRequested()) break;

            double avg = pop.stream().mapToDouble(BinaryIndividual::getFitness).average().orElse(0.0);
            BinaryIndividual best = bestOf(pop);

            if (best.getFitness() > globalBest.getFitness()) globalBest = best.clone();

            if (listener != null) {
                listener.onGeneration(new GenerationStats(
                        gen,
                        globalBest.getFitness(),
                        avg,
                        globalBest.genomeAsString()
                ));
            }

            // 2.1) Create next population (elitism + selection/crossover/mutation)
            pop.sort(Comparator.comparingDouble(BinaryIndividual::getFitness).reversed());
            List<BinaryIndividual> next = new ArrayList<>(cfg.populationSize());

            // elitism
            for (int i = 0; i < cfg.elitismCount(); i++) {
                next.add(pop.get(i).clone());
            }

            while (next.size() < cfg.populationSize()) {
                BinaryIndividual p1 = selection.select(pop, cfg.tournamentSize(), rnd);
                BinaryIndividual p2 = selection.select(pop, cfg.tournamentSize(), rnd);

                BinaryIndividual[] children;
                if (rnd.nextDouble() < cfg.crossoverRate()) {
                    children = crossover.crossover(p1, p2, rnd);
                } else {
                    children = new BinaryIndividual[]{p1.clone(), p2.clone()};
                }

                for (BinaryIndividual c : children) {
                    mutation.mutate(c, cfg.mutationRatePerGene(), rnd);
                    c.setFitness(fitness.evaluate(c));
                    next.add(c);
                    if (next.size() >= cfg.populationSize()) break;
                }
            }

            pop = next;
        }

        return globalBest;
    }

    private static BinaryIndividual bestOf(List<BinaryIndividual> pop) {
        return pop.stream().max(Comparator.comparingDouble(BinaryIndividual::getFitness))
                .orElseThrow(() -> new IllegalStateException("Empty population"));
    }
}
