package org.ProgettoFIA.gacore.operatori_genetici;


import org.ProgettoFIA.gacore.individuals.BinaryIndividual;

import java.util.List;
import java.util.Random;

public final class TournamentSelection {

    public BinaryIndividual select(List<BinaryIndividual> population, int tournamentSize, Random rnd) {
        BinaryIndividual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            BinaryIndividual candidate = population.get(rnd.nextInt(population.size()));
            if (best == null || candidate.getFitness() > best.getFitness()) best = candidate;
        }
        return best;
    }
}
