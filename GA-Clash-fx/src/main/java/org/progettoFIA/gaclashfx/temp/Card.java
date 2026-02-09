package org.progettoFIA.gaclashfx.temp;


import java.util.Objects;

/** Mock minimale per la GUI (id + name). */
public class Card {
    private final String id;
    private final String name;

    public Card(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

