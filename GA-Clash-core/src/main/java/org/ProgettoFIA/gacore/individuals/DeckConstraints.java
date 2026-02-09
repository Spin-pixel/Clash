package main.java.org.ProgettoFIA.gacore.individuals;

public class DeckConstraints {
    public String mandatoryCardId;      // Es: Deve contenere "t01"
    public Integer minTroops;           // Es: Almeno 4 truppe
    public Integer minSpells;           // Es: Almeno 2 incantesimi
    public Integer minBuildings;        // Es: Almeno 0 edifici
    public Integer minAirTarget;        // Es: Almeno 2 carte che colpiscono aria
    public Integer minBuildingTarget;   // Es: Almeno 1 win condition (target buildings)

    // Costruttore vuoto
    public DeckConstraints() {}
}