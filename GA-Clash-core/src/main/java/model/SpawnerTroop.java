package main.java.model;

public class SpawnerTroop extends Troop {
    private String idSpawnedTroop;
    private double spawnInterval;
    private int spawnCount;

    public SpawnerTroop(String id, String name, int elixirCost, int hitpoints, AttackScope attackScope,
                        MovementSpeed movementSpeed, int damage, double hitSpeed, double range,
                        double radius, boolean targetsOnlyBuildings, int groupSize,
                        String idSpawnedTroop, double spawnInterval, int spawnCount) {

        super(id, name, elixirCost, hitpoints, attackScope, movementSpeed, damage, hitSpeed,
                range, radius, targetsOnlyBuildings, groupSize);

        this.idSpawnedTroop = idSpawnedTroop;
        this.spawnInterval = spawnInterval;
        this.spawnCount = spawnCount;
    }

    // Nota: eredita getEfficiencyScore da Troop, ma potresti volerlo sovrascrivere qui.

    public String getIdSpawnedTroop() { return idSpawnedTroop; }
    public double getSpawnInterval() { return spawnInterval; }
    public int getSpawnCount() { return spawnCount; }

    @Override
    public String toString() {
        return "SpawnerTroop{" +
                "baseTroop=" + super.toString() +
                ", idSpawnedTroop='" + idSpawnedTroop + '\'' +
                ", spawnInterval=" + spawnInterval +
                ", spawnCount=" + spawnCount +
                '}';
    }
}