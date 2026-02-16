package agente.Simulated_Annealing;

import agente.Genetic_Algoritm.individuals.Deck; // per cardUsageCV()
import agente.Simulated_Annealing.Modello_di_transizione.Azione;
import agente.Simulated_Annealing.Stato_corrente.Stato;
import agente.Simulated_Annealing.Stato_corrente.Vincoli;
import agente.Simulated_Annealing.Stato_corrente.stato_iniziale;
import model.Card;
import model.Troop;

import java.util.*;

public class SA_core {

    public record Params(
            int numeroTry,
            int numCarte,
            double tempIniziale,
            double tempFin,
            double raff
    ) {
        public static Params defaults() {
            return new Params(
                    500,
                    101,
                    10000,
                    1,
                    0.25
            );
        }
    }

    /** Output originale */
    public record Output(Stato bestStato, String details, String log) {}

    /** Output esteso per metriche */
    public record OutputTrace(
            Stato bestStato,
            Deck bestDeck,                    // comodo per metriche
            String details,
            String log,
            List<Double> bestUtilityPerStep,  // per convergenceGeneration()
            List<Deck> bestDeckPerStep        // per cardUsageCV()
    ) {}

    /** Run originale: compatibile */
    public static Output run(List<Card> pool,
                             Vincoli vincoli,
                             double delta,
                             double desiredAvgElixir) {
        OutputTrace tr = runWithTrace(pool, vincoli, delta, desiredAvgElixir);
        return new Output(tr.bestStato(), tr.details(), tr.log());
    }

    /** Run con trace per metriche */
    public static OutputTrace runWithTrace(List<Card> pool,
                                           Vincoli vincoli,
                                           double delta,
                                           double desiredAvgElixir) {

        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(vincoli, "vincoli");

        StringBuilder log = new StringBuilder();
        List<Double> bestUtilityPerStep = new ArrayList<>();
        List<Deck> bestDeckPerStep = new ArrayList<>();

        if (pool.size() < Stato.SIZE_STATO) {
            String msg = "Pool carte troppo piccolo: servono almeno 8 carte, ne hai " + pool.size();
            return new OutputTrace(null, null, msg, msg, List.of(), List.of());
        }

        stato_iniziale starter = new stato_iniziale();
        Azione movement = new Azione();

        Stato start = starter.createStato(pool, vincoli, Params.defaults().numeroTry(), delta, desiredAvgElixir);

        // primo intorno
        List<Stato> neighborhood = movement.findNeighborhood(
                pool, start, vincoli,
                Params.defaults().numCarte(),
                Params.defaults().numeroTry(),
                delta, desiredAvgElixir
        );

        Collections.sort(neighborhood);
        neighborhood = neighborhood.reversed();
        Stato best = neighborhood.getFirst();

        // step 0
        bestUtilityPerStep.add(best.getUtility());
        bestDeckPerStep.add(toDeck(best));

        double time = Params.defaults().tempIniziale();

        // ciclo di raffreddamento
        while (time > Params.defaults().tempFin()) {

            neighborhood = movement.findNeighborhood(
                    pool, best, vincoli,
                    Params.defaults().numCarte(),
                    Params.defaults().numeroTry(),
                    delta, desiredAvgElixir
            );

            Collections.sort(neighborhood);
            neighborhood = neighborhood.reversed();
            Stato candidate = neighborhood.getFirst();

            // accetta se migliore o con probabilità
            if (acceptanceProbability(best.getUtility(), candidate.getUtility(), time) > Math.random()) {
                best = candidate;
            }

            // tracce per metriche
            bestUtilityPerStep.add(best.getUtility());
            bestDeckPerStep.add(toDeck(best));

            // raffreddamento (standard)
            time *= Params.defaults().raff();
        }

        String details = formatDetails(best, vincoli, delta);

        return new OutputTrace(
                best,
                toDeck(best),
                details,
                log.toString(),
                List.copyOf(bestUtilityPerStep),
                List.copyOf(bestDeckPerStep)
        );
    }

    /**
     * Acceptance prob coerente con MASSIMIZZAZIONE (utility più alta = migliore).
     */
    private static double acceptanceProbability(double currentScore, double neighborScore, double temp) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        return Math.exp((neighborScore - currentScore) / temp);
    }

    private static Deck toDeck(Stato s) {
        // snapshot minimo: copia lista carte
        return new Deck(new ArrayList<>(s.getCards()));
    }

    private static String formatDetails(Stato best, Vincoli c, double delta) {
        if (best == null) return "Nessun deck generato.";

        List<Card> cards = best.getCards();

        int spells = 0, buildings = 0, flying = 0, wincon = 0;
        for (Card card : cards) {
            if (card.getType() == Card.CardType.SPELL) spells++;
            if (card.getType() == Card.CardType.BUILDING) buildings++;
            if (card instanceof Troop t) {
                if (t.isFlying()) flying++;
                if (t.isTargetsOnlyBuildings()) wincon++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Funzione di Utilità: ").append(fmt(best.getUtility())).append("\n");
        sb.append("Delta: ").append(fmt(delta)).append("\n");

        sb.append("Vincoli (minimi):\n");
        sb.append("- Air >= ").append(c.nFlyingTroop).append("\n");
        sb.append("- Buildings >= ").append(c.nBuildings).append("\n");
        sb.append("- Spells >= ").append(c.nSpells).append("\n");
        sb.append("- Wincon >= ").append(c.nBuildingTarget).append("\n\n");

        sb.append("Conteggi deck:\n");
        sb.append("- Air = ").append(flying).append("\n");
        sb.append("- Buildings = ").append(buildings).append("\n");
        sb.append("- Spells = ").append(spells).append("\n");
        sb.append("- Wincon = ").append(wincon).append("\n\n");

        if (c.mandatoryCardsId != null && !c.mandatoryCardsId.isEmpty()) {
            sb.append("Carte obbligatorie: ").append(c.mandatoryCardsId).append("\n\n");
        }

        sb.append("Carte:\n");
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            sb.append(i + 1).append(") ").append(card.getName())
                    .append(" (").append(card.getId()).append(")\n");
        }

        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
