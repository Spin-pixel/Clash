package model;


public class DefensiveBuilding extends Card {
    private int hitpoints;
    private AttackScope attackScope;
    private int damage;
    private double hitSpeed;
    private double range;
    private double radius;

    public DefensiveBuilding(String id, String name, int elixirCost, CardTag tag, int hitpoints, AttackScope attackScope,
                             int damage, double hitSpeed, double range, double radius) {
        super(id, name, elixirCost, CardType.BUILDING, tag);
        this.hitpoints = hitpoints;
        this.attackScope = attackScope;
        this.damage = damage;
        this.hitSpeed = hitSpeed;
        this.range = range;
        this.radius = radius;
    }

    @Override
    public double getEfficiencyScore() {
        return (hitpoints + (damage * 10)) / (double) getElixirCost();
    }

    public int getHitpoints() { return hitpoints; }
    public AttackScope getAttackScope() { return attackScope; }
    public int getDamage() { return damage; }
    public double getHitSpeed() { return hitSpeed; }
    public double getRange() { return range; }
    public double getRadius() { return radius; }

    @Override
    public String toString() {
        return "DefensiveBuilding{" +
                "super=" + super.toString() +
                ", hitpoints=" + hitpoints +
                ", attackScope=" + attackScope +
                ", damage=" + damage +
                ", hitSpeed=" + hitSpeed +
                ", range=" + range +
                ", radius=" + radius +
                '}';
    }
}
