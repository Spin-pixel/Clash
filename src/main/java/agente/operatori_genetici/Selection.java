package agente.operatori_genetici;


import agente.individuals.Deck;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Selection {

    /**
     * Seleziona i migliori N deck dalla popolazione (Truncation Selection).
     * Ordina la lista per fitness decrescente e prende i primi nToSelect.
     *
     * @param population La lista completa dei deck.
     * @param nToSelect Il numero di deck da selezionare.
     * @return Una lista contenente i migliori deck.
     */
    public List<Deck> select(List<Deck> population, int nToSelect) {
        // Controllo di sicurezza: se chiedi più deck di quanti ne hai, li restituisci tutti.
        if (nToSelect > population.size()) {
            nToSelect = population.size();
        }

        // 1. Stream della popolazione
        return population.stream()
                // 2. Ordina in base alla fitness (Decrescente: dal più alto al più basso)
                .sorted(Comparator.comparingDouble(Deck::getFitness).reversed())
                // 3. Prendi solo i primi 'nToSelect' elementi
                .limit(nToSelect)
                // 4. Raccogli in una nuova lista modificabile
                .collect(Collectors.toList());
    }
}