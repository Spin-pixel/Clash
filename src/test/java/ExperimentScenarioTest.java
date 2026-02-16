import agente.Genetic_Algoritm.GA_core;
import agente.Genetic_Algoritm.individuals.Deck;
import agente.Genetic_Algoritm.individuals.DeckConstraints;
import metriche.EvaluationMetrics;
import model.Card;
import model.DefensiveBuilding;
import model.Spell;
import model.Troop;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test "di scenario":
 * - Scenario A: pool completo (baseline) + tracciamento curve fitness
 * - Scenario B: preferenze utente (vincoli rigidi)
 * - Scenario C: pool ridotto / collezione limitata + caso limite pool < 8
 */
public class ExperimentScenarioTest {

    private static final double DELTA = 1.0;
    private static final double T_DESIRED = 3.5;
    private static final double ALPHA = 0.95;
    private static final int PATIENCE = 5;

    // Scenario A: verifica che, con pool completo e senza vincoli extra, la run produca trace coerenti, bestFitness monotona (elitismo) e metriche calcolabili (g*, CV).
    @Test
    void scenarioA_poolCompleto_baseline_producesTraceAndMonotoneBest() {
        List<Card> pool = makePool();

        DeckConstraints c = new DeckConstraints(); // nessun vincolo (oltre a validità deck)

        GA_core.OutputTrace out = GA_core.runWithTrace(pool, c, DELTA, T_DESIRED);

        assertNotNull(out.bestDeck(), "Best deck nullo: " + out.details());
        assertNotNull(out.bestFitnessPerGen());
        assertNotNull(out.finalPopulation());

        // 100 generazioni + gen 0
        assertEquals(101, out.bestFitnessPerGen().size(), "Trace fitness attesa lunga 101");
        assertEquals(40, out.finalPopulation().size(), "Popolazione finale attesa 40 (Params.defaults)");

        // Proprietà importante con elitismo: bestFitness deve essere non-decrescente
        for (int i = 0; i < out.bestFitnessPerGen().size() - 1; i++) {
            double a = out.bestFitnessPerGen().get(i);
            double b = out.bestFitnessPerGen().get(i + 1);
            assertTrue(b + 1e-12 >= a, "bestFitness non monotona: g=" + i + " " + a + " -> " + b);
        }

        int gStar = EvaluationMetrics.convergenceGeneration(out.bestFitnessPerGen(), ALPHA, PATIENCE);
        assertTrue(gStar >= 0 && gStar <= 100);

        double cv = EvaluationMetrics.cardUsageCV(out.finalPopulation(), pool);
        assertTrue(Double.isFinite(cv));
        assertTrue(cv >= 0.0);
    }

    // Scenario B: verifica che, con vincoli rigidi (spell/building/flying/wincon + carta obbligatoria), il best deck prodotto li rispetti tutti.
    @Test
    void scenarioB_preferenzeUtente_vincoliRigidi_bestDeckRespectsConstraints() {
        List<Card> pool = makePool();

        DeckConstraints c = new DeckConstraints();
        c.mandatoryCardsId = List.of("spell_mandatory");
        c.nSpells = 2;
        c.nBuildings = 1;
        c.nFlyingTroop = 2;
        c.nBuildingTarget = 1;

        GA_core.OutputTrace out = GA_core.runWithTrace(pool, c, DELTA, T_DESIRED);

        assertNotNull(out.bestDeck(), "Best deck nullo: " + out.details());

        Deck best = out.bestDeck();
        assertTrue(containsId(best, "spell_mandatory"));

        assertEquals(2, countType(best, Card.CardType.SPELL));
        assertEquals(1, countType(best, Card.CardType.BUILDING));
        assertEquals(2, countFlyingTroops(best));
        assertEquals(1, countBuildingTargetTroops(best));
    }

    // Scenario C (pool ridotto ma valido): verifica che il best deck usi esclusivamente carte presenti nel pool ridotto (nessuna “carta inventata” fuori collezione).
    @Test
    void scenarioC_poolRidotto_returnsDeckUsingOnlyAvailableCards() {
        List<Card> pool = makePool();
        List<Card> reduced = pool.subList(0, 10); // pool ridotto ma >= 8

        DeckConstraints c = new DeckConstraints();

        GA_core.OutputTrace out = GA_core.runWithTrace(reduced, c, DELTA, T_DESIRED);

        assertNotNull(out.bestDeck());

        for (Card card : out.bestDeck().getCards()) {
            assertTrue(reduced.stream().anyMatch(p -> p.getId().equals(card.getId())),
                    "Carta non presente nel pool ridotto: " + card.getId());
        }
    }

    // Scenario C (pool troppo piccolo): verifica che con meno di 8 carte l'algoritmo segnali impossibilità (bestDeck null e trace/pop finali vuote).
    @Test
    void scenarioC_poolTroppoPiccolo_returnsNullBestDeckAndEmptyTraces() {
        List<Card> tiny = makePool().subList(0, 7); // < 8

        DeckConstraints c = new DeckConstraints();
        GA_core.OutputTrace out = GA_core.runWithTrace(tiny, c, DELTA, T_DESIRED);

        assertNull(out.bestDeck());
        assertTrue(out.bestFitnessPerGen().isEmpty());
        assertTrue(out.finalPopulation().isEmpty());
    }

    // ---------------- helper ----------------

    private static boolean containsId(Deck d, String id) {
        return d.getCards().stream().anyMatch(c -> c != null && id.equals(c.getId()));
    }

    private static int countType(Deck d, Card.CardType type) {
        int k = 0;
        for (Card c : d.getCards()) {
            if (c.getType() == type) k++;
        }
        return k;
    }

    private static int countFlyingTroops(Deck d) {
        int k = 0;
        for (Card c : d.getCards()) {
            if (c instanceof Troop t && t.isFlying()) k++;
        }
        return k;
    }

    private static int countBuildingTargetTroops(Deck d) {
        int k = 0;
        for (Card c : d.getCards()) {
            if (c instanceof Troop t && t.isTargetsOnlyBuildings()) k++;
        }
        return k;
    }

    private static List<Card> makePool() {
        List<Card> pool = new ArrayList<>();

        // --- Spells (6)
        pool.add(new Spell("spell_mandatory", "Mandatory Spell", 2, Card.CardTag.SUPPORT, 200, 80, 2.5));
        pool.add(new Spell("spell_1", "Spell 1", 2, Card.CardTag.CROWD_CONTROL, 120, 40, 2.0));
        pool.add(new Spell("spell_2", "Spell 2", 3, Card.CardTag.SUPPORT, 260, 90, 3.0));
        pool.add(new Spell("spell_3", "Spell 3", 4, Card.CardTag.SUPPORT, 400, 120, 2.5));
        pool.add(new Spell("spell_4", "Spell 4", 2, Card.CardTag.CROWD_CONTROL, 150, 50, 2.0));
        pool.add(new Spell("spell_5", "Spell 5", 6, Card.CardTag.SUPPORT, 800, 200, 2.0));

        // --- Buildings (3)
        pool.add(new DefensiveBuilding("bld_1", "Building 1", 3, Card.CardTag.SUPPORT, 900, Card.AttackScope.GROUND, 120, 1.0, 6.0, 0.0));
        pool.add(new DefensiveBuilding("bld_2", "Building 2", 4, Card.CardTag.SUPPORT, 1100, Card.AttackScope.AIR_GROUND, 140, 1.2, 6.0, 0.0));
        pool.add(new DefensiveBuilding("bld_3", "Building 3", 5, Card.CardTag.TANK_KILLER, 1300, Card.AttackScope.AIR_GROUND, 40, 0.4, 6.0, 0.0));

        // --- Troops (12) - includi flying e win condition
        // Flying (4)
        pool.add(new Troop("fly_1", "Flying 1", 3, Card.CardTag.SUPPORT, 600, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.FAST, 120, 1.1, 3.0, 0.4, false, 2, true));
        pool.add(new Troop("fly_2", "Flying 2", 4, Card.CardTag.SUPPORT, 750, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.MEDIUM, 160, 1.3, 3.5, 0.6, false, 1, true));
        pool.add(new Troop("fly_3", "Flying 3", 5, Card.CardTag.GLASS_CANNON, 500, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.FAST, 220, 1.2, 5.0, 0.0, false, 1, true));
        pool.add(new Troop("fly_4", "Flying 4", 2, Card.CardTag.SUPPORT, 300, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.VERY_FAST, 90, 1.0, 2.0, 0.0, false, 3, true));

        // Win condition / targetsOnlyBuildings (3) (ground)
        pool.add(new Troop("wc_1", "WinCon 1", 4, Card.CardTag.WIN_CONDITION, 1500, Card.AttackScope.GROUND, Card.MovementSpeed.FAST, 210, 1.5, 1.0, 0.0, true, 1, false));
        pool.add(new Troop("wc_2", "WinCon 2", 5, Card.CardTag.WIN_CONDITION, 2000, Card.AttackScope.GROUND, Card.MovementSpeed.MEDIUM, 260, 1.6, 1.0, 0.0, true, 1, false));
        pool.add(new Troop("wc_3", "WinCon 3", 2, Card.CardTag.WIN_CONDITION, 350, Card.AttackScope.GROUND, Card.MovementSpeed.VERY_FAST, 140, 1.2, 0.5, 0.0, true, 2, false));

        // Other ground troops (5)
        pool.add(new Troop("gr_1", "Ground 1", 3, Card.CardTag.MINI_TANK, 1800, Card.AttackScope.GROUND, Card.MovementSpeed.MEDIUM, 200, 1.2, 1.0, 0.6, false, 1, false));
        pool.add(new Troop("gr_2", "Ground 2", 4, Card.CardTag.TANK_KILLER, 1300, Card.AttackScope.GROUND, Card.MovementSpeed.FAST, 350, 1.8, 0.8, 0.0, false, 1, false));
        pool.add(new Troop("gr_3", "Ground 3", 2, Card.CardTag.SUPPORT, 500, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.FAST, 110, 1.0, 2.0, 0.2, false, 3, false));
        pool.add(new Troop("gr_4", "Ground 4", 6, Card.CardTag.TANK, 3500, Card.AttackScope.GROUND, Card.MovementSpeed.SLOW, 250, 2.0, 1.0, 0.0, false, 1, false));
        pool.add(new Troop("gr_5", "Ground 5", 3, Card.CardTag.CROWD_CONTROL, 900, Card.AttackScope.AIR_GROUND, Card.MovementSpeed.MEDIUM, 160, 1.4, 4.0, 0.8, false, 1, false));

        return pool;
    }
}
