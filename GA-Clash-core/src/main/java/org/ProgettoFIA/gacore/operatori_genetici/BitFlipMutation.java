package org.ProgettoFIA.gacore.operatori_genetici;


import org.ProgettoFIA.gacore.individuals.BinaryIndividual;

import java.util.Random;

public final class BitFlipMutation {

    // mutationRatePerGene: probabilità di flip per ciascun gene
    public void mutate(BinaryIndividual ind, double mutationRatePerGene, Random rnd) {
        boolean[] g = ind.genes();
        for (int i = 0; i < g.length; i++) {
            if (rnd.nextDouble() < mutationRatePerGene) g[i] = !g[i];
        }
    }
}
