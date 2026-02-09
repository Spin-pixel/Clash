package main.java.model;

public abstract class Card {

    public enum CardType {
        TROOP, BUILDING, SPELL
    }

    public enum AttackScope {
        AIR_GROUND, GROUND
    }

    public enum MovementSpeed {
        SLOW, MEDIUM, FAST, VERY_FAST
    }

    private String id;
    private String name;
    private int elixirCost;
    private CardType type;

    public Card(String id, String name, int elixirCost, CardType type) {
        this.id = id;
        this.name = name;
        this.elixirCost = elixirCost;
        this.type = type;
    }

    // Metodo astratto richiesto dall'UML
    public abstract double getEfficiencyScore();

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getElixirCost() { return elixirCost; }
    public CardType getType() { return type; }

    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", elixirCost=" + elixirCost +
                ", type=" + type +
                '}';
    }
}