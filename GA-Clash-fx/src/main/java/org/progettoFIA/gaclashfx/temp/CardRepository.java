package org.progettoFIA.gaclashfx.temp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mock repository: restituisce una lista di carte fittizie per test UI. */
public final class CardRepository {

    private static final List<Card> ALL = new ArrayList<>();

    private CardRepository() {}

    public static List<Card> getAll() {
        if (ALL.isEmpty()) seed();
        return Collections.unmodifiableList(ALL);
    }

    /** Per quando arriverà il parser vero: puoi rimpiazzare i mock al volo. */
    public static void populate(List<Card> cards) {
        ALL.clear();
        if (cards != null) ALL.addAll(cards);
    }

    private static void seed() {
        ALL.clear();
        ALL.add(new Card("c01", "Domatore"));
        ALL.add(new Card("c02", "Tronco"));
        ALL.add(new Card("c03", "Palla di Fuoco"));
        ALL.add(new Card("c04", "Moschettiere"));
        ALL.add(new Card("c05", "Golem"));
        ALL.add(new Card("c06", "Minatore"));
        ALL.add(new Card("c07", "Veleno"));
        ALL.add(new Card("c08", "Scheletrini"));
        ALL.add(new Card("c09", "Scarica"));
        ALL.add(new Card("c10", "Tesla"));
    }
}
