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

    // Verifica che convergenceGeneration gestisca input null o vuoti senza crash e ritorni un valore di default (0).
    @Test
    void convergenceGeneration_handlesNullAndEmpty() {
        assertEquals(0, EvaluationMetrics.convergenceGeneration(null, 0.95, 5));
        assertEquals(0, EvaluationMetrics.convergenceGeneration(List.of(), 0.95, 5));
    }

    // Verifica che convergenceGeneration ritorni la prima generazione g* in cui la fitness supera la soglia e resta stabile per "patience" generazioni.
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

    // Verifica che convergenceGeneration non segnali convergenza se la soglia viene superata ma subito dopo la fitness scende (evita falsi positivi) e rispetti "patience".
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

    // Verifica che convergenceGeneration, se non trova un tratto stabile per la patience richiesta, ritorni l'ultima generazione come fallback.
    @Test
    void convergenceGeneration_returnsLastIfNeverStable() {
        // best finale = 1.00, soglia 0.99, mai stabile per 3 generazioni
        List<Double> curve = List.of(
                0.10, 0.40, 0.70, 0.95, 0.98, 0.99, 0.98, 1.00
        );
        int gStar = EvaluationMetrics.convergenceGeneration(curve, 0.99, 3);
        assertEquals(curve.size() - 1, gStar);
    }

    // Verifica che cardUsageCV ritorni 0 quando l'uso delle carte è perfettamente uniforme rispetto all'universo (nessuna varianza).
    @Test
    void cardUsageCV_isZeroWhenPerfectlyUniformOverUniverse() {
        List<Card> universe = makeSimpleUniverse(8);

        Deck d = new Deck(universe); // un deck che usa tutte le carte una sola volta
        double cv = EvaluationMetrics.cardUsageCV(List.of(d), universe);

        assertEquals(0.0, cv, 1e-12);
    }

    // Verifica che cardUsageCV risulti alto quando una carta domina e l'esperimento usa solo un piccolo sottoinsieme dell'universo (molte carte restano a frequenza 0).
    @Test
    void cardUsageCV_isHighWhenOneCardDominates() {
        // Universo realistico (molte carte non usate => CV alto)
        List<Card> universe = makeSimpleUniverse(100);

        Card dominant = universe.get(0);
        List<Deck> decks = new ArrayList<>();

        // Usiamo sempre e solo un sottoinsieme piccolo (prime 12) per accentuare la polarizzazione
        int subset = 12;

        for (int i = 0; i < 20; i++) {
            List<Card> cards = new ArrayList<>();
            cards.add(dominant);

            int start = 1 + (i % (subset - 7)); // start in [1..5] così start+6 <= 11
            for (int k = 0; k < 7; k++) {
                cards.add(universe.get(start + k));
            }
            decks.add(new Deck(cards));
        }

        double cv = EvaluationMetrics.cardUsageCV(decks, universe);

        assertTrue(cv > 1.0, "CV atteso alto per polarizzazione, trovato: " + cv);
    }


    // Verifica che cardUsageCV ignori carte non presenti nell'universo e non produca NaN/Infinity, restando sempre finito e non negativo.
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

        while (out.size() < n && i < n) {
            out.add(new Spell("spell_" + i, "Spell " + i, 2 + (i % 4),
                    Card.CardTag.SUPPORT, 100 + i, 50 + i, 2.0));
            i++;
        }

        int b = 0;
        while (out.size() < n && b < 2) {
            out.add(new DefensiveBuilding("bld_" + b, "Building " + b, 4,
                    Card.CardTag.SUPPORT, 1000, Card.AttackScope.AIR_GROUND, 100, 1.0, 6.0, 0.0));
            b++;
        }

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
