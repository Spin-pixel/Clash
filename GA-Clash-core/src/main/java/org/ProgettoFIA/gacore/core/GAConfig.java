package org.ProgettoFIA.gacore.core;

public record GAConfig(
        int populationSize,
        int chromosomeLength,
        int tournamentSize,
        double crossoverRate,
        double mutationRatePerGene,
        int elitismCount,
        int maxGenerations,
        long randomSeed
) {
    public GAConfig {
        if (populationSize < 2) throw new IllegalArgumentException("populationSize >= 2");
        if (chromosomeLength < 1) throw new IllegalArgumentException("chromosomeLength >= 1");
        if (tournamentSize < 2) throw new IllegalArgumentException("tournamentSize >= 2");
        if (elitismCount < 0 || elitismCount >= populationSize)
            throw new IllegalArgumentException("elitismCount in [0, populationSize-1]");
        if (maxGenerations < 1) throw new IllegalArgumentException("maxGenerations >= 1");
        if (crossoverRate < 0 || crossoverRate > 1) throw new IllegalArgumentException("crossoverRate in [0,1]");
        if (mutationRatePerGene < 0 || mutationRatePerGene > 1)
            throw new IllegalArgumentException("mutationRatePerGene in [0,1]");
    }
}
