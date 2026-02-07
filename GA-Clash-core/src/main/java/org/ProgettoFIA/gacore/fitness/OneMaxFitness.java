package org.ProgettoFIA.gacore.fitness;


import org.ProgettoFIA.gacore.individuals.BinaryIndividual;

public final class OneMaxFitness {

    // Massimizziamo il numero di bit a 1
    public double evaluate(BinaryIndividual ind) {
        int ones = 0;
        for (boolean b : ind.genes()) if (b) ones++;
        return ones;
    }
}
