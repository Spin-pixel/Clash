package org.ProgettoFIA.gacore.api;

public record GenerationStats(
        int generation,
        double bestFitness,
        double avgFitness,
        String bestGenome
) {}
