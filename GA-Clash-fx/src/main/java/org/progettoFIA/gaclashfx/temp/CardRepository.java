package org.progettoFIA.gaclashfx.temp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CardRepository {

    private static final List<Card> ALL = new ArrayList<>();
    private CardRepository() {}

    public static List<Card> getAll() {
        if (ALL.isEmpty()) seed();
        return Collections.unmodifiableList(ALL);
    }

    public static void populate(List<Card> cards) {
        ALL.clear();
        if (cards != null) ALL.addAll(cards);
    }

    private static void seed() {
        ALL.clear();
        ALL.add(new Card("c01", "Domatore", "/img/c01.png"));
        ALL.add(new Card("c02", "Tronco", "/img/c02.png"));
        ALL.add(new Card("c03", "Palla di Fuoco", "/img/c03.png"));
        ALL.add(new Card("c04", "Moschettiere", "/img/c04.png"));
        ALL.add(new Card("c05", "Golem", "/img/c05.png"));
        ALL.add(new Card("c06", "Minatore", "/img/c06.png"));
        ALL.add(new Card("c07", "Veleno", "/img/c07.png"));
        ALL.add(new Card("c08", "Scheletrini", "/img/c08.png"));
        ALL.add(new Card("c09", "Scarica", "/img/c09.png"));
        ALL.add(new Card("c10", "Tesla", "/img/c10.png"));
    }
}
