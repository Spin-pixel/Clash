package model;


public abstract class Card {

    public enum CardTag {
        WIN_CONDITION, TANK_KILLER, TANK, MINI_TANK, GLASS_CANNON, CROWD_CONTROL, SUPPORT
    }

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
    private CardTag tag;

    public Card(String id, String name, int elixirCost, CardType type, CardTag tag) {
        this.id = id;
        this.name = name;
        this.elixirCost = elixirCost;
        this.type = type;
        this.tag = tag;
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
                ", tag=" + tag +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        // Usa Objects.equals per evitare NullPointerException se l'id fosse null
        return java.util.Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        // Importante: delega l'hash alla stringa dell'ID
        return java.util.Objects.hash(id);
    }
}