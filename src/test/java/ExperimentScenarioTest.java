import agente.Genetic_Algoritm.GA_core;
import agente.Genetic_Algoritm.individuals.Deck;
import agente.Genetic_Algoritm.individuals.DeckConstraints;
import agente.Simulated_Annealing.SA_core;
import agente.Simulated_Annealing.Stato_corrente.Vincoli;
import metriche.EvaluationMetrics;
import model.Card;
import model.DefensiveBuilding;
import model.Spell;
import model.Troop;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test "di scenario":
 * - Scenario A: pool completo (baseline) + tracciamento curve fitness
 * - Scenario B: preferenze utente (vincoli rigidi)
 * - Scenario C: pool ridotto / collezione limitata + caso limite pool < 8
 *
 * MODIFICA:
 * Ogni test, oltre alle assert, genera 2 PNG (uno GA e uno SA) con un istogramma delle metriche:
 * - Convergenza g* (alpha/patience)
 * - Card Usage CV
 *
 * I file vengono salvati nella stessa cartella sorgente del test:
 *   src/test/java[/<package>]
 */
public class ExperimentScenarioTest {

    static {
        // per evitare problemi in ambienti headless
        System.setProperty("java.awt.headless", "true");
    }

    private static final double DELTA = 1.0;
    private static final double T_DESIRED = 3.5;
    private static final double ALPHA = 0.95;
    private static final int PATIENCE = 5;

    // Scenario A: pool completo + trace coerenti + bestFitness monotona (elitismo) + metriche calcolabili
    @Test
    void scenarioA_poolCompleto_baseline_producesTraceAndMonotoneBest() throws Exception {
        List<Card> pool = makePool();

        DeckConstraints c = new DeckConstraints(); // nessun vincolo extra (oltre validità)

        // GA
        GA_core.OutputTrace gaOut = GA_core.runWithTrace(pool, c, DELTA, T_DESIRED);

        assertNotNull(gaOut.bestDeck(), "Best deck nullo: " + gaOut.details());
        assertNotNull(gaOut.bestFitnessPerGen());
        assertNotNull(gaOut.finalPopulation());

        // 100 generazioni + gen 0
        assertEquals(101, gaOut.bestFitnessPerGen().size(), "Trace fitness attesa lunga 101");
        assertEquals(40, gaOut.finalPopulation().size(), "Popolazione finale attesa 40 (Params.defaults)");

        // Con elitismo: bestFitness non-decrescente
        for (int i = 0; i < gaOut.bestFitnessPerGen().size() - 1; i++) {
            double a = gaOut.bestFitnessPerGen().get(i);
            double b = gaOut.bestFitnessPerGen().get(i + 1);
            assertTrue(b + 1e-12 >= a, "bestFitness non monotona: g=" + i + " " + a + " -> " + b);
        }

        int gStarGA = EvaluationMetrics.convergenceGeneration(gaOut.bestFitnessPerGen(), ALPHA, PATIENCE);
        assertTrue(gStarGA >= 0 && gStarGA <= 100);

        double cvGA = EvaluationMetrics.cardUsageCV(gaOut.finalPopulation(), pool);
        assertTrue(Double.isFinite(cvGA));
        assertTrue(cvGA >= 0.0);

        // SA (trace + metriche)
        Vincoli v = toVincoli(c);
        SA_core.OutputTrace saOut = SA_core.runWithTrace(pool, v, DELTA, T_DESIRED);

        assertNotNull(saOut.bestStato(), "Best stato SA nullo: " + saOut.details());
        assertNotNull(saOut.bestUtilityPerStep());
        assertNotNull(saOut.bestDeckPerStep());

        int gStarSA = EvaluationMetrics.convergenceGeneration(saOut.bestUtilityPerStep(), ALPHA, PATIENCE);
        assertTrue(gStarSA >= 0);

        double cvSA = EvaluationMetrics.cardUsageCV(saOut.bestDeckPerStep(), pool);
        assertTrue(Double.isFinite(cvSA));
        assertTrue(cvSA >= 0.0);

        // PNG (due istogrammi: GA e SA)
        saveMetricsCharts("scenarioA_poolCompleto_baseline", gStarGA, cvGA, gStarSA, cvSA);
    }

    // Scenario B: vincoli rigidi + carta obbligatoria. Verifico che GA e SA rispettino i vincoli MINIMI.
    @Test
    void scenarioB_preferenzeUtente_vincoliRigidi_bestDeckRespectsConstraints() throws Exception {
        List<Card> pool = makePool();

        DeckConstraints c = new DeckConstraints();
        c.mandatoryCardsId = List.of("spell_mandatory");
        c.nSpells = 2;
        c.nBuildings = 1;
        c.nFlyingTroop = 2;
        c.nBuildingTarget = 1;

        // GA
        GA_core.OutputTrace gaOut = GA_core.runWithTrace(pool, c, DELTA, T_DESIRED);
        assertNotNull(gaOut.bestDeck(), "Best deck GA nullo: " + gaOut.details());

        Deck gaBest = gaOut.bestDeck();
        assertTrue(containsId(gaBest, "spell_mandatory"));

        // NB: considero vincoli come MINIMI (>=), non "esattamente ="
        assertTrue(countType(gaBest, Card.CardType.SPELL) >= c.nSpells);
        assertTrue(countType(gaBest, Card.CardType.BUILDING) >= c.nBuildings);
        assertTrue(countFlyingTroops(gaBest) >= c.nFlyingTroop);
        assertTrue(countBuildingTargetTroops(gaBest) >= c.nBuildingTarget);

        int gStarGA = EvaluationMetrics.convergenceGeneration(gaOut.bestFitnessPerGen(), ALPHA, PATIENCE);
        double cvGA = EvaluationMetrics.cardUsageCV(gaOut.finalPopulation(), pool);

        // SA
        Vincoli v = toVincoli(c);
        SA_core.OutputTrace saOut = SA_core.runWithTrace(pool, v, DELTA, T_DESIRED);
        assertNotNull(saOut.bestStato(), "Best stato SA nullo: " + saOut.details());
        Deck saBestDeck = saOut.bestDeck(); // fornito dal trace

        assertNotNull(saBestDeck);
        assertTrue(containsId(saBestDeck, "spell_mandatory"));
        assertTrue(countType(saBestDeck, Card.CardType.SPELL) >= c.nSpells);
        assertTrue(countType(saBestDeck, Card.CardType.BUILDING) >= c.nBuildings);
        assertTrue(countFlyingTroops(saBestDeck) >= c.nFlyingTroop);
        assertTrue(countBuildingTargetTroops(saBestDeck) >= c.nBuildingTarget);

        int gStarSA = EvaluationMetrics.convergenceGeneration(saOut.bestUtilityPerStep(), ALPHA, PATIENCE);
        double cvSA = EvaluationMetrics.cardUsageCV(saOut.bestDeckPerStep(), pool);

        // PNG
        saveMetricsCharts("scenarioB_vincoliRigidi", gStarGA, cvGA, gStarSA, cvSA);
    }

    // Scenario C (pool ridotto ma valido): GA e SA devono usare solo carte presenti nel pool ridotto.
    @Test
    void scenarioC_poolRidotto_returnsDeckUsingOnlyAvailableCards() throws Exception {
        List<Card> pool = makePool();
        List<Card> reduced = pool.subList(0, 10); // >= 8

        DeckConstraints c = new DeckConstraints();

        // GA
        GA_core.OutputTrace gaOut = GA_core.runWithTrace(reduced, c, DELTA, T_DESIRED);
        assertNotNull(gaOut.bestDeck());

        for (Card card : gaOut.bestDeck().getCards()) {
            assertTrue(reduced.stream().anyMatch(p -> p.getId().equals(card.getId())),
                    "GA: carta non presente nel pool ridotto: " + card.getId());
        }

        int gStarGA = EvaluationMetrics.convergenceGeneration(gaOut.bestFitnessPerGen(), ALPHA, PATIENCE);
        double cvGA = EvaluationMetrics.cardUsageCV(gaOut.finalPopulation(), reduced);

        // SA
        Vincoli v = toVincoli(c);
        SA_core.OutputTrace saOut = SA_core.runWithTrace(reduced, v, DELTA, T_DESIRED);
        assertNotNull(saOut.bestDeck());

        for (Card card : saOut.bestDeck().getCards()) {
            assertTrue(reduced.stream().anyMatch(p -> p.getId().equals(card.getId())),
                    "SA: carta non presente nel pool ridotto: " + card.getId());
        }

        int gStarSA = EvaluationMetrics.convergenceGeneration(saOut.bestUtilityPerStep(), ALPHA, PATIENCE);
        double cvSA = EvaluationMetrics.cardUsageCV(saOut.bestDeckPerStep(), reduced);

        // PNG
        saveMetricsCharts("scenarioC_poolRidotto", gStarGA, cvGA, gStarSA, cvSA);
    }

    // Scenario C (pool troppo piccolo): GA e SA devono segnalare impossibilità (best null e trace vuote).
    @Test
    void scenarioC_poolTroppoPiccolo_returnsNullBestDeckAndEmptyTraces() throws Exception {
        List<Card> tiny = makePool().subList(0, 7); // < 8

        DeckConstraints c = new DeckConstraints();

        // GA
        GA_core.OutputTrace gaOut = GA_core.runWithTrace(tiny, c, DELTA, T_DESIRED);
        assertNull(gaOut.bestDeck());
        assertTrue(gaOut.bestFitnessPerGen().isEmpty());
        assertTrue(gaOut.finalPopulation().isEmpty());

        // SA
        Vincoli v = toVincoli(c);
        SA_core.OutputTrace saOut = SA_core.runWithTrace(tiny, v, DELTA, T_DESIRED);
        assertNull(saOut.bestStato());
        assertTrue(saOut.bestUtilityPerStep().isEmpty());
        assertTrue(saOut.bestDeckPerStep().isEmpty());

        // per questo scenario non ha senso calcolare g*/CV: salvo comunque un grafico "vuoto" (0,0)
        saveMetricsCharts("scenarioC_poolTroppoPiccolo", 0, 0.0, 0, 0.0);
    }

    // ===========================
    // PNG BAR CHART (HELPERS)
    // ===========================

    private static void saveMetricsCharts(String scenario, int gStarGA, double cvGA, int gStarSA, double cvSA) throws Exception {
        Path dir = testSourceDir();
        Files.createDirectories(dir);

        // un istogramma per algoritmo (2 barre: g* e CV)
        saveHistogramPng(
                dir.resolve(scenario + "_GA_metrics.png"),
                "GA - " + scenario,
                new String[]{"g*", "CV"},
                new double[]{gStarGA, cvGA},
                new String[]{"%d", "%.4f"},
                new double[]{Math.max(1, 100), Math.max(1.0, cvGA)} // ref per scalare le barre
        );

        saveHistogramPng(
                dir.resolve(scenario + "_SA_metrics.png"),
                "SA - " + scenario,
                new String[]{"g*", "CV"},
                new double[]{gStarSA, cvSA},
                new String[]{"%d", "%.4f"},
                new double[]{Math.max(1, 500), Math.max(1.0, cvSA)} // ref indicativa per SA (step potenzialmente > 100)
        );
    }

    private static void saveHistogramPng(Path file,
                                         String title,
                                         String[] labels,
                                         double[] values,
                                         String[] formats,
                                         double[] maxRefs) throws Exception {

        int w = 900;
        int h = 420;

        int left = 80, right = 40, top = 60, bottom = 90;
        int plotW = w - left - right;
        int plotH = h - top - bottom;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // bg
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            // title
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString(title, left, 30);

            // axes
            int x0 = left;
            int y0 = top + plotH;
            g.setStroke(new BasicStroke(2f));
            g.drawLine(x0, y0, x0 + plotW, y0); // x
            g.drawLine(x0, y0, x0, top);        // y

            int n = labels.length;
            int gap = 90;
            int barW = Math.max(60, (plotW - gap * (n + 1)) / n);

            g.setFont(new Font("SansSerif", Font.PLAIN, 14));

            for (int i = 0; i < n; i++) {
                double v = values[i];
                double ref = maxRefs[i] <= 0 ? 1.0 : maxRefs[i];
                double norm = Math.max(0.0, Math.min(1.0, v / ref));
                int barH = (int) Math.round(norm * (plotH - 20));

                int x = x0 + gap + i * (barW + gap);
                int y = y0 - barH;

                // bar fill
                g.setColor(new Color(70, 130, 180)); // steel-ish (semplice e leggibile)
                g.fillRect(x, y, barW, barH);

                // bar border
                g.setColor(Color.BLACK);
                g.drawRect(x, y, barW, barH);

                // label
                String lab = labels[i];
                int lw = g.getFontMetrics().stringWidth(lab);
                g.drawString(lab, x + (barW - lw) / 2, y0 + 22);

                // value text
                String txt;
                if ("%d".equals(formats[i])) {
                    txt = String.format(formats[i], (int) Math.round(v));
                } else {
                    txt = String.format(formats[i], v);
                }
                int tw = g.getFontMetrics().stringWidth(txt);
                g.drawString(txt, x + (barW - tw) / 2, y - 10);
            }

            // footer hint
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.setColor(new Color(60, 60, 60));
            g.drawString("g*: più basso = convergenza più rapida | CV: più basso = uso carte più uniforme", left, h - 20);

            ImageIO.write(img, "png", file.toFile());
        } finally {
            g.dispose();
        }
    }

    private static Path testSourceDir() {
        String pkg = ExperimentScenarioTest.class.getPackageName(); // "" se default package
        Path p = Paths.get("src", "test", "java");
        if (pkg != null && !pkg.isBlank()) {
            p = p.resolve(pkg.replace('.', '/'));
        }
        return p;
    }

    // ===========================
    // CONSTRAINT MAPPING (GA -> SA)
    // ===========================

    private static Vincoli toVincoli(DeckConstraints dc) {
        Vincoli v = new Vincoli();
        v.mandatoryCardsId = dc.mandatoryCardsId;
        v.nSpells = dc.nSpells;
        v.nBuildings = dc.nBuildings;
        v.nFlyingTroop = dc.nFlyingTroop;
        v.nBuildingTarget = dc.nBuildingTarget;
        return v;
    }

    // ===========================
    // HELPERS (assert)
    // ===========================

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

    // ===========================
    // POOL BUILDER
    // ===========================

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

        // --- Troops (12)
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
