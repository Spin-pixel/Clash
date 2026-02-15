package agente.GA.operatori_genetici;



import agente.GA.individuals.Deck;
import agente.GA.individuals.DeckConstraints;
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

        Random rand = new Random();
        //Ogni deck ha mutationeRate probabilità di essere mutato
        for(Deck d: population){
            if(rand.nextDouble() < mutationRate)
                mutateSingleDeck(d,nGenesToMutate,constraints);
        }
    }

    /**
     * Prova a mutare un singolo deck. Se la mutazione crea un deck non valido
     * (non rispetta i constraints), riprova fino a MAX_ATTEMPTS.
     */
    private void mutateSingleDeck(Deck deck, int nGenes, DeckConstraints constraints) {

        //Prendo le carte del deck
        List<Card> originalCards = new ArrayList<>(deck.getCards()); // Backup

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Crea una copia modificabile delle carte attuali
            List<Card> candidateCards = new ArrayList<>(originalCards);

            // Identifica le carte che POSSONO essere rimosse (escludi le mandatory)
            List<Card> removableCards = candidateCards.stream()
                    .filter(c -> constraints.mandatoryCardsId == null || !constraints.mandatoryCardsId.contains(c.getId()))
                    .collect(Collectors.toList());

            if (removableCards.size() < nGenes) break; // Sicurezza se non ci sono abbastanza carte da togliere

            // Rimuovi nGenes carte a caso
            for (int k = 0; k < nGenes; k++) {
                Card toRemove = removableCards.get(random.nextInt(removableCards.size()));
                candidateCards.remove(toRemove);
                removableCards.remove(toRemove); // Rimuovi dalla lista locale per non riselezionarla
            }

            // Aggiungi nGenes carte NUOVE
            int cardsAdded = 0;
            while (cardsAdded < nGenes) {
                Card randomCard = allCards.get(random.nextInt(allCards.size()));

                // CHECK DUPLICATI: La carta non deve essere già nel deck candidato
                if (!candidateCards.contains(randomCard)) {
                    candidateCards.add(randomCard);
                    cardsAdded++;
                }
            }

            // Verifica se il nuovo set di carte rispetta i vincoli
            if (checkConstraints(candidateCards, constraints)) {
                deck.setCards(candidateCards);
                return;
            }
        }

    }

    /**
     * Verifica i vincoli usando la logica dei bucket fornita.
     */
    private boolean checkConstraints(List<Card> cards, DeckConstraints constraints) {

        int countSpells = 0;
        int countBuildings = 0; // Difensivi + Spawner
        int countFlying = 0;
        int countBuildingTarget = 0; // Win conditions

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

                boolean isFlying = false;
                boolean targetsOnlyBuildings = false;

                if (c instanceof Troop) {
                    //Logica Truppe Volanti
                    isFlying = ((Troop) c).isFlying();

                    //Logica Truppe Only Buildings
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
