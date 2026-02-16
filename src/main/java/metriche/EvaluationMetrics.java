package metriche;

import agente.Genetic_Algoritm.individuals.Deck;
import model.Card;

import java.util.*;

/**
 * Metriche di valutazione per GA su deck Clash Royale.
 *
 * 1) ACG / ConvergenceGeneration:
 *    prima generazione g* che raggiunge alpha del best finale e ci resta per "patience" generazioni.
 *
 * 2) CardUsageCV:
 *    coefficiente di variazione delle frequenze d'uso delle carte su un insieme di deck S.
 *
 * Nota onesta: per ACG serve la curva bestFitnessPerGen; il tuo GA_core attuale non la esporta.
 */
public final class EvaluationMetrics {

    private EvaluationMetrics() {}

    /**
     * Convergence Generation (g*).
     *
     * @param bestFitnessPerGen lista con il best fitness a ogni generazione (indice = generazione)
     * @param alpha             percentuale che deve raggiungere la fitness di una gen rispetto alla finale  es. 0.95 => 95% del best finale
     * @param patience          numero di generazioni consecutive sopra soglia
     * @return g* (prima gen che soddisfa), altrimenti last
     */
    public static int convergenceGeneration(List<Double> bestFitnessPerGen, double alpha, int patience) {
        if (bestFitnessPerGen == null || bestFitnessPerGen.isEmpty()) return 0;

        //prendo l'indice dell'ultima gen e la miglior fitness raggiunta in quest'ultima
        int last = bestFitnessPerGen.size() - 1;
        double finalBest = bestFitnessPerGen.get(last);

        //soglia minima da raggiungere che le gen devono superare
        double threshold = alpha * finalBest;
        int p = Math.max(0, patience);

        //controllo tutte le gen per trovarne almeno una sopra la soglia
        for (int g = 0; g <= last; g++) {
            //se è piu bassa della soglia non la considero altrimenti potrebbe essere g*
            if (bestFitnessPerGen.get(g) < threshold) continue;

            //deve rimanere sopra la soglia anche nel numero di gen successive definite dalla patience
            boolean stable = true;
            for (int k = 0; k < p; k++) {
                int idx = g + k;
                if (idx > last) break;
                if (bestFitnessPerGen.get(idx) < threshold) {
                    stable = false;
                    break;
                }
            }
            if (stable) return g;
        }
        return last;
    }

    /**
     * CardUsageCV:
     * freq(id) = occorrenze(id) / totalSlots
     * CV = std(freq) / mean(freq)
     *
     * @param decks     insieme S (es. top-K finali, o popolazione finale)
     * @param universe  tutte le carte possibili (serve per non “barare” ignorando quelle mai usate)
     */
    //CV sta per coefficiente di variazione
    public static double cardUsageCV(List<Deck> decks, Collection<Card> universe) {
        if (decks == null || decks.isEmpty()) return 0.0;
        if (universe == null || universe.isEmpty()) return 0.0;

        // universo per id
        Map<String, Integer> count = new HashMap<>();
        for (Card c : universe) {
            if (c != null && c.getId() != null) {
                count.put(c.getId(), 0);
            }
        }
        if (count.isEmpty()) return 0.0;

        //conteggio occorrenze nei deck
        int totalSlots = 0;
        for (Deck d : decks) {
            if (d == null) continue;
            for (Card c : d.getCards()) {
                if (c == null || c.getId() == null) continue;
                if (!count.containsKey(c.getId())) continue; // ignora carte fuori universo
                count.merge(c.getId(), 1, Integer::sum);
                //conteggio quante carte valide ho processato
                totalSlots++;
            }
        }
        if (totalSlots == 0) return 0.0;

        int finalTotalSlots = totalSlots;
        //per ogni carta calcolo la frequenza con cui compare nei mazzi
        double[] freqs = count.values().stream()
                .mapToDouble(v -> v / (double) finalTotalSlots)
                .toArray();

        //mean sarà la media aritmetica delle frequenze d'uso delle carte
        double mean = Arrays.stream(freqs).average().orElse(0.0);
        if (mean <= 0.0) return 0.0;

        //calcolo la varianza
        double var = 0.0;
        for (double f : freqs) {
            double dx = f - mean;
            var += dx * dx;
        }
        var /= freqs.length;

        //calcolo la deviazione standard
        double std = Math.sqrt(var);

        //dividendo la deviazione standard per la media della frequenza d'uso
        return std / mean;
    }

    /* -----------------------------------------------------------------------
       PATCH NECESSARIA PER USARE ACG SUL TUO GA (COMMENTATA)
       ----------------------------------------------------------------------- */

    /*
    // Nel tuo GA_core.run(), ti basta aggiungere:
    List<Double> bestFitnessPerGen = new ArrayList<>();

    // Dopo init:
    bestFitnessPerGen.add(population.getFirst().getFitness());

    // Dentro al for round:
    bestFitnessPerGen.add(population.getFirst().getFitness());

    // E poi esporla (senza rompere la GUI) con un nuovo record e metodo:
    public record OutputTrace(Deck bestDeck, String details, String log, List<Double> bestFitnessPerGen) {}

    public static OutputTrace runWithTrace(...) { ... return new OutputTrace(best, details, log, bestFitnessPerGen); }
    */
}
