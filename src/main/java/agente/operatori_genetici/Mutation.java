package agente.operatori_genetici;



import agente.individuals.Deck;
import agente.individuals.DeckConstraints;
import model.Card;
import model.Troop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Mutation {

    private final List<Card> allCards;
    private final Random random;
    private final int MAX_ATTEMPTS = 10; // Quante volte riprovare se la mutazione rompe i vincoli

    public Mutation(List<Card> allCards) {
        this.allCards = allCards;
        this.random = new Random();
    }

    /**
     * Muta una percentuale della popolazione.
     *
     * @param population La lista di Deck (popolazione corrente).
     * @param mutationRate Percentuale di deck da mutare (0.0 a 1.0). Es: 0.2 = 20%.
     * @param nGenesToMutate Numero di carte da cambiare in ogni deck selezionato.
     * @param constraints I vincoli da rispettare.
     */
    public void mutateGeneration(List<Deck> population, double mutationRate, int nGenesToMutate, DeckConstraints constraints) {
        int populationSize = population.size();

        // Calcola quanti deck mutare in base al rate
        int numberOfDecksToMutate = (int) (populationSize * mutationRate);

        // Creiamo una lista di indici mescolati per scegliere a caso QUALI deck mutare
        List<Integer> deckIndices = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            deckIndices.add(i);
        }
        Collections.shuffle(deckIndices);

        // Muta i primi N deck della lista mescolata
        for (int i = 0; i < numberOfDecksToMutate; i++) {
            int indexToMutate = deckIndices.get(i);
            Deck originalDeck = population.get(indexToMutate);

            // Tenta di mutare il deck
            mutateSingleDeck(originalDeck, nGenesToMutate, constraints);
        }
    }

    /**
     * Prova a mutare un singolo deck. Se la mutazione crea un deck non valido
     * (non rispetta i constraints), riprova fino a MAX_ATTEMPTS.
     */
    private void mutateSingleDeck(Deck deck, int nGenes, DeckConstraints constraints) {
        List<Card> originalCards = new ArrayList<>(deck.getCards()); // Backup

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // 1. Crea una copia modificabile delle carte attuali
            List<Card> candidateCards = new ArrayList<>(originalCards);

            // 2. Identifica le carte che POSSONO essere rimosse (escludi le mandatory)
            List<Card> removableCards = candidateCards.stream()
                    .filter(c -> constraints.mandatoryCardsId == null || !constraints.mandatoryCardsId.contains(c.getId()))
                    .collect(Collectors.toList());

            if (removableCards.size() < nGenes) break; // Sicurezza se non ci sono abbastanza carte da togliere

            // 3. Rimuovi nGenes carte a caso
            for (int k = 0; k < nGenes; k++) {
                Card toRemove = removableCards.get(random.nextInt(removableCards.size()));
                candidateCards.remove(toRemove);
                removableCards.remove(toRemove); // Rimuovi dalla lista locale per non riselezionarla
            }

            // 4. Aggiungi nGenes carte NUOVE
            int cardsAdded = 0;
            while (cardsAdded < nGenes) {
                Card randomCard = allCards.get(random.nextInt(allCards.size()));

                // CHECK DUPLICATI: La carta non deve essere già nel deck candidato
                if (!candidateCards.contains(randomCard)) {
                    candidateCards.add(randomCard);
                    cardsAdded++;
                }
            }

            // 5. Verifica se il nuovo set di carte rispetta i vincoli
            if (checkConstraints(candidateCards, constraints)) {
                // Se valido, applica la mutazione al Deck originale e esci
                deck.setCards(candidateCards);
                // System.out.println("Mutazione riuscita al tentativo " + (attempt + 1));
                return;
            }
        }

        // System.out.println("Mutazione fallita dopo " + MAX_ATTEMPTS + " tentativi. Deck invariato.");
    }

    /**
     * Verifica i vincoli usando la logica dei bucket fornita.
     */
    private boolean checkConstraints(List<Card> cards, DeckConstraints constraints) {
        // 1. Check Mandatory Cards (Fast fail)
        if (constraints.mandatoryCardsId != null) {
            List<String> currentIds = cards.stream().map(Card::getId).collect(Collectors.toList());
            for (String mandatoryId : constraints.mandatoryCardsId) {
                if (!currentIds.contains(mandatoryId)) return false;
            }
        }

        // 2. Inizializza i contatori
        int countSpells = 0;
        int countBuildings = 0; // Difensivi + Spawner
        int countFlying = 0;
        int countBuildingTarget = 0; // Win conditions

        // 3. Logica dei Bucket (Adattata dal tuo snippet per contare)
        for (Card c : cards) {

            // Logica Building
            if (c.getType() == Card.CardType.BUILDING) {
                countBuildings++;
            }

            // Logica Spell
            else if (c.getType() == Card.CardType.SPELL) {
                countSpells++;
            }

            // Logica Truppe
            else if (c.getType() == Card.CardType.TROOP) {
                // Nota: In Java bisogna fare il cast o usare metodi getter se l'interfaccia Card non ha questi metodi.
                // Assumo che tu abbia dei metodi getter comuni o che facciamo cast sicuri.

                boolean isFlying = false;
                boolean targetsOnlyBuildings = false;

                // Controllo safely i tipi specifici per estrarre le proprietà
                if (c instanceof Troop) {
                    isFlying = ((Troop) c).isFlying();
                    targetsOnlyBuildings = ((Troop) c).isTargetsOnlyBuildings();
                }

                if (isFlying) countFlying++;
                if (targetsOnlyBuildings) countBuildingTarget++;
            }
        }

        // 4. Confronto finale con i constraints (se il constraint è null, lo ignoriamo)
        if (constraints.nSpells != null && countSpells < constraints.nSpells) return false;
        if (constraints.nBuildings != null && countBuildings < constraints.nBuildings) return false;
        if (constraints.nFlyingTroop != null && countFlying < constraints.nFlyingTroop) return false;
        if (constraints.nBuildingTarget != null && countBuildingTarget < constraints.nBuildingTarget) return false;

        return true;
    }
}
