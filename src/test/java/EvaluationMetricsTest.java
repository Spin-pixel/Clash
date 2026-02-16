import agente.Genetic_Algoritm.individuals.Deck;
import metriche.EvaluationMetrics;
import model.Card;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests per EvaluationMetrics usando le tue classi reali Deck e Card.
 *
 * Nota: Card è abstract, quindi qui uso una "TestCard" minimale che estende Card.
 */
public class EvaluationMetricsTest {

    @Test
    void convergenceGeneration_shouldReturnGenBeforeLastOnPlateauCurve() {
        double alpha = 0.95;
        int patience = 5;

        List<Double> curve = simulateBestFitnessCurveWithPlateau(60, 10);
        int gStar = EvaluationMetrics.convergenceGeneration(curve, alpha, patience);

        int last = curve.size() - 1;
        assertTrue(gStar >= 0 && gStar <= last);
        assertTrue(gStar < last, "Con plateau finale mi aspetto convergenza prima dell'ultima gen");

        double finalBest = curve.get(last);
        assertTrue(curve.get(gStar) >= alpha * finalBest);
    }

    @Test
    void convergenceGeneration_shouldReturnLastWhenFinalBestNotPositive() {
        List<Double> curve = Arrays.asList(-1.0, -0.2, 0.0);
        int gStar = EvaluationMetrics.convergenceGeneration(curve, 0.95, 5);
        assertEquals(curve.size() - 1, gStar);
    }

    @Test
    void cardUsageCV_shouldBeHigherForPolarizedDecksThanBalancedDecks() {
        // Universo carte (molte più di quelle usate, così il CV è “onesto”)
        List<Card> universe = buildUniverseCards();

        List<Deck> polarized = buildPolarizedDecks(universe);
        List<Deck> balanced = buildBalancedDecks(universe);

        double cvPolar = EvaluationMetrics.cardUsageCV(polarized, universe);
        double cvBal = EvaluationMetrics.cardUsageCV(balanced, universe);

        assertTrue(cvPolar >= 0.0 && cvBal >= 0.0);
        assertTrue(cvPolar > cvBal, "Mi aspetto CV più alto nei deck polarizzati");
    }

    @Test
    void cardUsageCV_shouldReturnZeroOnEmptyInputs() {
        List<Card> universe = buildUniverseCards();

        assertEquals(0.0, EvaluationMetrics.cardUsageCV(Collections.emptyList(), universe));
        assertEquals(0.0, EvaluationMetrics.cardUsageCV(null, universe));
        assertEquals(0.0, EvaluationMetrics.cardUsageCV(buildBalancedDecks(universe), Collections.emptyList()));
        assertEquals(0.0, EvaluationMetrics.cardUsageCV(buildBalancedDecks(universe), null));
    }

    /* --------------------------------------------------------------------
       Helpers (curve)
       -------------------------------------------------------------------- */

    private static List<Double> simulateBestFitnessCurveWithPlateau(int rampGens, int plateauGens) {
        Random rnd = new Random(42);
        List<Double> curve = new ArrayList<>();
        double v = 10.0;

        for (int g = 0; g < rampGens; g++) {
            v += 2.0 + rnd.nextDouble() * 0.8;
            curve.add(v);
        }
        for (int g = 0; g < plateauGens; g++) {
            v += rnd.nextDouble() * 0.2;
            curve.add(v);
        }
        return curve;
    }

    /* --------------------------------------------------------------------
       Helpers (cards + decks)
       -------------------------------------------------------------------- */

    /**
     * Costruisce un universo minimo di carte test (id unici).
     * Card è abstract: uso TestCard che estende Card e implementa getEfficiencyScore().
     */
    /**
     * Costruisce un universo minimo di carte test (id unici) usando gli stessi ID del JSON.
     * Card è abstract: uso TestCard che estende Card e implementa getEfficiencyScore().
     * <p>
     * NOTA: qui l’obiettivo del test è solo avere ID coerenti (per map/Deck) e tipi/tag validi.
     */
    private static List<Card> buildUniverseCards() {
        List<Card> cards = new ArrayList<>();

        // TROOP (id come nel JSON)
        cards.add(new TestCard("golem", "Golem", 8, Card.CardType.TROOP, Card.CardTag.TANK));
        cards.add(new TestCard("giant", "Giant", 5, Card.CardType.TROOP, Card.CardTag.WIN_CONDITION));
        cards.add(new TestCard("hog_rider", "Hog Rider", 4, Card.CardType.TROOP, Card.CardTag.WIN_CONDITION));
        cards.add(new TestCard("miner", "Miner", 3, Card.CardType.TROOP, Card.CardTag.WIN_CONDITION));
        cards.add(new TestCard("balloon", "Balloon", 5, Card.CardType.TROOP, Card.CardTag.WIN_CONDITION));

        cards.add(new TestCard("mini_pekka", "Mini P.E.K.K.A", 4, Card.CardType.TROOP, Card.CardTag.TANK_KILLER));
        cards.add(new TestCard("musketeer", "Musketeer", 4, Card.CardType.TROOP, Card.CardTag.GLASS_CANNON));
        cards.add(new TestCard("wizard", "Wizard", 5, Card.CardType.TROOP, Card.CardTag.GLASS_CANNON));
        cards.add(new TestCard("valkyrie", "Valkyrie", 4, Card.CardType.TROOP, Card.CardTag.MINI_TANK));
        cards.add(new TestCard("guards", "Guards", 3, Card.CardType.TROOP, Card.CardTag.SUPPORT));

        // aggiungo un paio di carte “neutre” per fare deck diversi senza duplicati
        cards.add(new TestCard("knight", "Knight", 3, Card.CardType.TROOP, Card.CardTag.MINI_TANK));
        cards.add(new TestCard("archers", "Archers", 3, Card.CardType.TROOP, Card.CardTag.SUPPORT));

        // SPELL (id come nel JSON)
        cards.add(new TestCard("zap", "Zap", 2, Card.CardType.SPELL, Card.CardTag.CROWD_CONTROL));
        cards.add(new TestCard("fireball", "Fireball", 4, Card.CardType.SPELL, Card.CardTag.SUPPORT));
        cards.add(new TestCard("poison", "Poison", 4, Card.CardType.SPELL, Card.CardTag.SUPPORT));
        cards.add(new TestCard("arrows", "Arrows", 3, Card.CardType.SPELL, Card.CardTag.SUPPORT));
        cards.add(new TestCard("the_log", "The Log", 2, Card.CardType.SPELL, Card.CardTag.CROWD_CONTROL));

        return cards;
    }

    private static List<Deck> buildPolarizedDecks(List<Card> universe) {
        // Polarizzati: tanti deck ripetono sempre hog_rider + zap + fireball (+ core di supporto)
        Map<String, Card> byId = toMapById(universe);

        List<Deck> decks = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            decks.add(new Deck(List.of(
                    req(byId, "hog_rider"),
                    req(byId, "zap"),
                    req(byId, "fireball"),
                    req(byId, "valkyrie"),
                    req(byId, "musketeer"),
                    req(byId, "the_log"),
                    req(byId, "arrows"),
                    req(byId, "knight")
            )));
        }

        for (int i = 0; i < 10; i++) {
            decks.add(new Deck(List.of(
                    req(byId, "hog_rider"),
                    req(byId, "miner"),
                    req(byId, "zap"),
                    req(byId, "poison"),
                    req(byId, "guards"),
                    req(byId, "musketeer"),
                    req(byId, "the_log"),
                    req(byId, "arrows")
            )));
        }

        return decks;
    }

    private static List<Deck> buildBalancedDecks(List<Card> universe) {
        // Bilanciati: archetipi diversi e meno overlap “obbligato”
        Map<String, Card> byId = toMapById(universe);

        List<Deck> decks = new ArrayList<>();

        decks.add(new Deck(List.of(
                req(byId, "golem"), req(byId, "wizard"), req(byId, "zap"), req(byId, "fireball"),
                req(byId, "valkyrie"), req(byId, "musketeer"), req(byId, "arrows"), req(byId, "the_log")
        )));

        decks.add(new Deck(List.of(
                req(byId, "giant"), req(byId, "mini_pekka"), req(byId, "zap"), req(byId, "fireball"),
                req(byId, "musketeer"), req(byId, "guards"), req(byId, "arrows"), req(byId, "the_log")
        )));

        decks.add(new Deck(List.of(
                req(byId, "miner"), req(byId, "balloon"), req(byId, "zap"), req(byId, "poison"),
                req(byId, "musketeer"), req(byId, "valkyrie"), req(byId, "arrows"), req(byId, "the_log")
        )));

        decks.add(new Deck(List.of(
                req(byId, "hog_rider"), req(byId, "knight"), req(byId, "zap"), req(byId, "fireball"),
                req(byId, "archers"), req(byId, "valkyrie"), req(byId, "arrows"), req(byId, "the_log")
        )));

        // replica con piccole variazioni (sempre 8 carte, no duplicati)
        for (int i = 0; i < 8; i++) {
            decks.add(new Deck(List.of(
                    req(byId, "giant"), req(byId, "wizard"), req(byId, "zap"), req(byId, "poison"),
                    req(byId, "mini_pekka"), req(byId, "archers"), req(byId, "arrows"), req(byId, "the_log")
            )));
        }

        return decks;
    }

    private static Map<String, Card> toMapById(List<Card> universe) {
        Map<String, Card> map = new HashMap<>();
        for (Card c : universe) {
            if (c != null && c.getId() != null) map.put(c.getId(), c);
        }
        return map;
    }

    /**
     * Helper: se manca un id, fallisci subito con errore chiaro.
     * (Meglio che avere null dentro Deck e beccarsi eccezioni “misteriose”).
     */
    private static Card req(Map<String, Card> byId, String id) {
        Card c = byId.get(id);
        if (c == null) {
            throw new IllegalStateException("Carta con id '" + id + "' mancante nell'universo di test.");
        }
        return c;
    }

    /**
     * Implementazione minima per rendere Card istanziabile nei test.
     * Non tocca il tuo codice di produzione.
     */
    private static class TestCard extends Card {
        public TestCard(String id, String name, int elixirCost, CardType type, CardTag tag) {
            super(id, name, elixirCost, type, tag);
        }

        @Override
        public double getEfficiencyScore() {
            return 0.0; // per i test delle metriche non serve la logica reale
        }
    }
}

