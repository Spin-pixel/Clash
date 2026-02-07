package org.ProgettoFIA.gacore.api;

@FunctionalInterface
public interface GenerationListener {
    void onGeneration(GenerationStats stats);
}
