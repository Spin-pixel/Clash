import agente.Genetic_Algoritm.individuals.Deck;
import metriche.EvaluationMetrics;
import model.Card;
import model.DefensiveBuilding;
import model.Spell;
import model.Troop;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluationMetricsTest {

    @Test
    void convergenceGeneration_handlesNullAndEmpty() {
        assertEquals(0, EvaluationMetrics.convergenceGeneration(null, 0.95, 5));
        assertEquals(0, EvaluationMetrics.convergenceGeneration(List.of(), 0.95, 5));
    }

    @Test
    void convergenceGeneration_returnsFirstStableGeneration() {
        // best finale = 1.00, soglia alpha=0.95 => 0.95
        // raggiunge 0.95 a g=4 e rimane sopra soglia per almeno 3 generazioni (g=4,5,6)
        List<Double> curve = List.of(
                0.10, 0.30, 0.60, 0.94, 0.95, 0.96, 0.97, 1.00
        );
        int gStar = EvaluationMetrics.convergenceGeneration(curve, 0.95, 3);
        assertEquals(4, gStar);
    }

    @Test
    void convergenceGeneration_respectsPatienceAndAvoidsFalsePositive() {
        // best finale = 1.00, soglia 0.95
        // a g=3 supera soglia ma poi scende (falso positivo), converge davvero a g=5
        List<Double> curve = List.of(
                0.10, 0.40, 0.70, 0.96, 0.90, 0.95, 0.96, 1.00
        );
        int gStar = EvaluationMetrics.convergenceGeneration(curve, 0.95, 2);
        assertEquals(5, gStar);
    }

    @Test
    void convergenceGeneration_returnsLastIfNeverStable() {
        // best finale = 1.00, soglia 0.99, mai stabile per 3 generazioni
        List<Double> curve = List.of(
                0.10, 0.40, 0.70, 0.95, 0.98, 0.99, 0.98, 1.00
        );
        int gStar = EvaluationMetrics.convergenceGeneration(curve, 0.99, 3);
        assertEquals(curve.size() - 1, gStar);
    }

    @Test
    void cardUsageCV_isZeroWhenPerfectlyUniformOverUniverse() {
        List<Card> universe = makeSimpleUniverse(8);

        Deck d = new Deck(universe); // un deck che usa tutte le carte una sola volta
        double cv = EvaluationMetrics.cardUsageCV(List.of(d), universe);

        assertEquals(0.0, cv, 1e-12);
    }

    @Test
    void cardUsageCV_isHighWhenOneCardDominates() {
        List<Card> universe = makeSimpleUniverse(12);

        Card dominant = universe.get(0);
        List<Deck> decks = new ArrayList<>();

        // 20 deck: stessa carta dominante sempre presente (in Clash non ci sono doppioni nel singolo deck)
        for (int i = 0; i < 20; i++) {
            List<Card> cards = new ArrayList<>();
            cards.add(dominant);
            // riempiamo con 7 carte diverse prendendole in modo "rotante"
            int start = 1 + (i % (universe.size() - 8));
            for (int k = 0; k < 7; k++) {
                cards.add(universe.get(start + k));
            }
            decks.add(new Deck(cards));
        }

        double cv = EvaluationMetrics.cardUsageCV(decks, universe);

        // Non fissiamo un valore esatto (dipende dal bilanciamento), ma deve essere sensibilmente > 0
        assertTrue(cv > 1.0, "CV atteso alto per polarizzazione, trovato: " + cv);
    }

    @Test
    void cardUsageCV_ignoresCardsOutsideUniverse() {
        List<Card> universe = makeSimpleUniverse(8);
        List<Card> deckCards = new ArrayList<>(universe);

        // carta fuori universo
        deckCards.set(0, new Spell("OUT", "OutSpell", 2, Card.CardTag.SUPPORT, 100, 50, 2.0));

        Deck d = new Deck(deckCards);
        double cv = EvaluationMetrics.cardUsageCV(List.of(d), universe);

        // In questo caso 1 slot viene ignorato, ma CV deve rimanere finito e >= 0
        assertTrue(Double.isFinite(cv));
        assertTrue(cv >= 0.0);
    }

    /**
     * Crea un universo di carte "semplici" (spell/building/troop) con ID unici.
     * Serve a evitare dipendenze da file esterni (dataset JSON) nei test unitari.
     */
    private static List<Card> makeSimpleUniverse(int n) {
        List<Card> out = new ArrayList<>();
        int i = 0;

        // qualche spell
        while (out.size() < n && i < n) {
            out.add(new Spell("spell_" + i, "Spell " + i, 2 + (i % 4),
                    Card.CardTag.SUPPORT, 100 + i, 50 + i, 2.0));
            i++;
        }

        // se serve, aggiungi building
        int b = 0;
        while (out.size() < n && b < 2) {
            out.add(new DefensiveBuilding("bld_" + b, "Building " + b, 4,
                    Card.CardTag.SUPPORT, 1000, Card.AttackScope.AIR_GROUND, 100, 1.0, 6.0, 0.0));
            b++;
        }

        // se serve, aggiungi troop
        int t = 0;
        while (out.size() < n) {
            out.add(new Troop("trp_" + t, "Troop " + t, 3 + (t % 3),
                    Card.CardTag.SUPPORT, 500 + 10*t, Card.AttackScope.AIR_GROUND,
                    Card.MovementSpeed.MEDIUM, 100 + 5*t, 1.2, 3.0, 0.3,
                    false, 1, (t % 2 == 0)));
            t++;
        }

        // per sicurezza: per un Deck servono 8 carte
        if (out.size() < 8) throw new IllegalStateException("Universe troppo piccolo: " + out.size());

        return out;
    }
}
