package main.java.model;

public class SpawnerBuilding extends Card {
    private int hitpoints;
    private String idSpawnedTroop;
    private double spawnInterval;
    private int spawnCount;

    public SpawnerBuilding(String id, String name, int elixirCost, int hitpoints,
                           String idSpawnedTroop, double spawnInterval, int spawnCount) {
        super(id, name, elixirCost, CardType.BUILDING);
        this.hitpoints = hitpoints;
        this.idSpawnedTroop = idSpawnedTroop;
        this.spawnInterval = spawnInterval;
        this.spawnCount = spawnCount;
    }

    @Override
    public double getEfficiencyScore() {
        return (hitpoints + (spawnCount * 50)) / (double) getElixirCost();
    }

    public int getHitpoints() { return hitpoints; }
    public String getIdSpawnedTroop() { return idSpawnedTroop; }
    public double getSpawnInterval() { return spawnInterval; }
    public int getSpawnCount() { return spawnCount; }

    @Override
    public String toString() {
        return "SpawnerBuilding{" +
                "super=" + super.toString() +
                ", hitpoints=" + hitpoints +
                ", idSpawnedTroop='" + idSpawnedTroop + '\'' +
                ", spawnInterval=" + spawnInterval +
                ", spawnCount=" + spawnCount +
                '}';
    }
}
