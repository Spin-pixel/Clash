package agente.Simulated_Annealing.Stato_corrente;



import agente.Genetic_Algoritm.individuals.Deck;
import agente.Simulated_Annealing.Funzione_Utilità.funzione_utilità;
import model.Card;
import model.Troop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Posso definirlo in modo randomico come per i Deck
 * @see agente.Genetic_Algoritm.initializer.Initializer
 *
 * */
public class stato_iniziale {

    private Random random = new Random();

    private funzione_utilità funz =new funzione_utilità();

    //mi permette di evitare loop infiniti
    private int flag = 0;

    public Stato createStato(List<Card> pool,Vincoli vincoli, int numeroTry) {
        List<Card> cards = new ArrayList<Card>();
        // Usiamo un Set di Stringhe per tracciare gli ID presi
        java.util.Set<String> takenIds = new java.util.HashSet<>();

        // 1. Gestione Carte Obbligatorie
        // Assumo che constraints.mandatoryCardsId sia List<String>
        if (vincoli.mandatoryCardsId != null) {
            for (String mandatoryId : vincoli.mandatoryCardsId) {
                // Cerchiamo la carta nel pool
                for (Card c : pool) {

                    if (c.getId().equals(mandatoryId) && !takenIds.contains(mandatoryId)) {
                        cards.add(c);
                        takenIds.add(mandatoryId);
                        break; // Presa! Passiamo al prossimo ID obbligatorio
                    }
                }
            }
        }

        // 2. Mescoliamo una copia del pool per pescare le rimanenti
        List<Card> shuffledPool = new ArrayList<>(pool);
        Collections.shuffle(shuffledPool, random);

        // 3. Riempiamo fino a 8 carte
        for (Card candidate : shuffledPool) {
            // Stop se siamo arrivati a 8
            if (cards.size() >= Deck.DECK_SIZE) {
                break;
            }

            // Aggiungiamo SOLO se l'ID non è già nel set
            String cId = candidate.getId();
            if (!takenIds.contains(cId)) {
                cards.add(candidate);
                takenIds.add(cId);
            }
        }
        Stato stato = new Stato(cards);
        if(isValid(stato,vincoli)) {
            flag=0;
            stato.setUtility(funz.Totale_FU(stato));
            return stato;
        }
        else {
            if(flag == numeroTry) {
                flag=0;
                return null;
            }
            flag++;
            return createStato(pool, vincoli,numeroTry);
        }
    }

    private boolean isValid(Stato stato, Vincoli vincoli) {
        List<Card> cards = stato.getCards();

        // 1. Vincolo Carte Obbligatorie (Mandatory)
        if (vincoli.mandatoryCardsId != null && !vincoli.mandatoryCardsId.isEmpty()) {

            // SOLUZIONE:
            // Estraiamo prima tutti gli ID dalle carte presenti nel mazzo corrente.
            // Usiamo .map(Card::getId) per trasformare la lista di oggetti Card in una lista di Stringhe.
            List<String> deckIds = cards.stream()
                    .map(Card::getId)
                    .toList(); // Usa .collect(Collectors.toList()) se sei su Java < 16

            // Ora possiamo usare containsAll perché stiamo confrontando String con String
            boolean hasAllMandatoryCards = deckIds.containsAll(vincoli.mandatoryCardsId);

            // Se manca anche solo una delle carte obbligatorie, il controllo fallisce
            if (!hasAllMandatoryCards) return false;
        }

        // Contatori
        int buildingCount = 0;
        int spellCount = 0;
        int flyingTroopCount = 0;
        int buildingTargetCount = 0;


        for (Card c : cards) {
            // Conta Tipi
            if (c.getType() == Card.CardType.BUILDING) buildingCount++;
            else if (c.getType() == Card.CardType.SPELL) spellCount++;

            // Conta Air Target
            // Attenzione: Valido solo per Troop e DefensiveBuilding
            if (c instanceof Troop) {
                Troop t = (Troop) c;
                if (t.isFlying()) {
                    flyingTroopCount++;
                }
            }

            // Conta Building Target (Solo Troops hanno questo campo specifico nel tuo modello)
            if (c instanceof Troop) {
                if (((Troop) c).isTargetsOnlyBuildings()) {
                    buildingTargetCount++;
                }
            }
        }

        // Verifica conteggi
        if (vincoli.nBuildings != null && buildingCount != vincoli.nBuildings) return false;
        if (vincoli.nSpells != null && spellCount != vincoli.nSpells) return false;
        if (vincoli.nFlyingTroop != null && flyingTroopCount != vincoli.nFlyingTroop) return false;
        if (vincoli.nBuildingTarget != null && buildingTargetCount != vincoli.nBuildingTarget) return false;

        return true;
    }
}
