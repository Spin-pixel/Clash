package org.ProgettoFIA.gacore.operatori_genetici;


import org.ProgettoFIA.gacore.individuals.BinaryIndividual;

import java.util.Random;

public final class SinglePointCrossover {

    public BinaryIndividual[] crossover(BinaryIndividual p1, BinaryIndividual p2, Random rnd) {
        boolean[] g1 = p1.genes();
        boolean[] g2 = p2.genes();
        int n = Math.min(g1.length, g2.length);
        if (n < 2) return new BinaryIndividual[]{p1.clone(), p2.clone()};

        int cut = 1 + rnd.nextInt(n - 1); // [1, n-1]

        BinaryIndividual c1 = p1.clone();
        BinaryIndividual c2 = p2.clone();

        boolean[] cg1 = c1.genes();
        boolean[] cg2 = c2.genes();

        for (int i = cut; i < n; i++) {
            boolean tmp = cg1[i];
            cg1[i] = cg2[i];
            cg2[i] = tmp;
        }
        return new BinaryIndividual[]{c1, c2};
    }
}
