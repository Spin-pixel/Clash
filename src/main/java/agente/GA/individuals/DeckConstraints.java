package agente.GA.individuals;


import java.util.List;

public class DeckConstraints {
    public List<String> mandatoryCardsId;      // Es: Deve contenere "t01"
    public Integer nSpells;           // Es: Almeno 2 incantesimi
    public Integer nBuildings;        // Es: Almeno 0 edifici
    public Integer nFlyingTroop;        // Es: Almeno 2 carte che volano
    public Integer nBuildingTarget;   // Es: Almeno 1 win condition (target buildings)

    // Costruttore vuoto
    public DeckConstraints() {}
}