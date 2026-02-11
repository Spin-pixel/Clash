package org.progettoFIA.gaclashfx.temp;

import java.util.Objects;

public class Card {
    private final String id;
    private final String name;
    private final String imagePath; // es: "/cards/c01.png"

    public Card(String id, String name, String imagePath) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }

    @Override public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
