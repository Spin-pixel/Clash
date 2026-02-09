package main.java.model;

public class Spell extends Card {
    private int damage;
    private int crownTowerDamage;
    private double radius;

    public Spell(String id, String name, int elixirCost, int damage, int crownTowerDamage, double radius) {
        super(id, name, elixirCost, CardType.SPELL);
        this.damage = damage;
        this.crownTowerDamage = crownTowerDamage;
        this.radius = radius;
    }

    @Override
    public double getEfficiencyScore() {
        return (double) damage / getElixirCost();
    }

    public int getDamage() { return damage; }
    public int getCrownTowerDamage() { return crownTowerDamage; }
    public double getRadius() { return radius; }

    @Override
    public String toString() {
        return "Spell{" +
                "super=" + super.toString() +
                ", damage=" + damage +
                ", crownTowerDamage=" + crownTowerDamage +
                ", radius=" + radius +
                '}';
    }
}