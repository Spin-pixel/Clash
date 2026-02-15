package agente.Simulated_Annealing.Stato_corrente;

import java.util.List;


/**
 * Implementa i vincoli utili a
 * @see Stato in modo analogo a come fa
 * @see agente.Genetic_Algoritm.individuals.DeckConstraints per
 * @see agente.Genetic_Algoritm.individuals.Deck
 * */
public class Vincoli {
    public List<String> mandatoryCardsId;      // Es: Deve contenere "t01"
    public Integer nSpells;           // Es: Almeno 2 incantesimi
    public Integer nBuildings;        // Es: Almeno 0 edifici
    public Integer nFlyingTroop;        // Es: Almeno 2 carte che volano
    public Integer nBuildingTarget;   // Es: Almeno 1 win condition (target buildings)

    public Vincoli() {}
}
