package org.ProgettoFIA.gacore.individuals;

import java.util.Arrays;
import java.util.Random;

public final class BinaryIndividual implements Cloneable {
    private final boolean[] genes;
    private double fitness;

    public BinaryIndividual(boolean[] genes) {
        this.genes = genes;
    }

    public static BinaryIndividual random(int length, Random rnd) {
        boolean[] g = new boolean[length];
        for (int i = 0; i < length; i++) g[i] = rnd.nextBoolean();
        return new BinaryIndividual(g);
    }

    public boolean[] genes() { return genes; }

    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }

    public String genomeAsString() {
        StringBuilder sb = new StringBuilder(genes.length);
        for (boolean b : genes) sb.append(b ? '1' : '0');
        return sb.toString();
    }

    @Override
    public BinaryIndividual clone() {
        try {
            super.clone(); // validate Cloneable
            boolean[] copied = Arrays.copyOf(this.genes, this.genes.length);
            BinaryIndividual out = new BinaryIndividual(copied);
            out.fitness = this.fitness;
            return out;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        return genomeAsString() + " | fitness=" + fitness;
    }
}
