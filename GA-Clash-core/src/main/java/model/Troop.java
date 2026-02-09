package main.java.model;

public class Troop extends Card {
    private int hitpoints;
    private AttackScope attackScope;
    private MovementSpeed movementSpeed;
    private int damage;
    private double hitSpeed;
    private double range;
    private double radius;
    private boolean targetsOnlyBuildings;
    private int groupSize;

    public Troop(String id, String name, int elixirCost, int hitpoints, AttackScope attackScope,
                 MovementSpeed movementSpeed, int damage, double hitSpeed, double range,
                 double radius, boolean targetsOnlyBuildings, int groupSize) {
        super(id, name, elixirCost, CardType.TROOP);
        this.hitpoints = hitpoints;
        this.attackScope = attackScope;
        this.movementSpeed = movementSpeed;
        this.damage = damage;
        this.hitSpeed = hitSpeed;
        this.range = range;
        this.radius = radius;
        this.targetsOnlyBuildings = targetsOnlyBuildings;
        this.groupSize = groupSize;
    }

    @Override
    public double getEfficiencyScore() {
        return (hitpoints * damage) / (double) getElixirCost();
    }

    // Getters
    public int getHitpoints() { return hitpoints; }
    public AttackScope getAttackScope() { return attackScope; }
    public MovementSpeed getMovementSpeed() { return movementSpeed; }
    public int getDamage() { return damage; }
    public double getHitSpeed() { return hitSpeed; }
    public double getRange() { return range; }
    public double getRadius() { return radius; }
    public boolean isTargetsOnlyBuildings() { return targetsOnlyBuildings; }
    public int getGroupSize() { return groupSize; }

    @Override
    public String toString() {
        return "Troop{" +
                "super=" + super.toString() +
                ", hitpoints=" + hitpoints +
                ", attackScope=" + attackScope +
                ", movementSpeed=" + movementSpeed +
                ", damage=" + damage +
                ", hitSpeed=" + hitSpeed +
                ", range=" + range +
                ", radius=" + radius +
                ", targetsOnlyBuildings=" + targetsOnlyBuildings +
                ", groupSize=" + groupSize +
                '}';
    }
}