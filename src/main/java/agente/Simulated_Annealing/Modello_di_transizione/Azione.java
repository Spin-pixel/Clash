package agente.Simulated_Annealing.Modello_di_transizione;

import agente.Genetic_Algoritm.individuals.DeckConstraints;
import agente.Simulated_Annealing.Funzione_Utilità.funzione_utilità;
import agente.Simulated_Annealing.Stato_corrente.Stato;
import agente.Simulated_Annealing.Stato_corrente.Vincoli;
import model.Card;
import model.Troop;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Azione {

    private int flag = 0;
    private int numTry;
    private Random rand = new Random();
    private funzione_utilità funz=new funzione_utilità();

    /**
     * Individuo tutti i vicini di una data popolazione.
     * */
    public List<Stato> findNeighborhood(List<Card> pool, Stato stato, Vincoli vincoli, int qty,int numTry) {
        List<Stato> neighborhood = new ArrayList<>();
        this.numTry = numTry;
        for (int i = 0; i < 8; i++) {
            neighborhood = Stream.concat(neighborhood.stream(), replace(pool, stato, vincoli, qty, i).stream()).collect(Collectors.toList());
        }
        return neighborhood;
    }

    private List<Stato> replace(List<Card> pool, Stato stato, Vincoli vincoli, int qty, int pos) {
        List<Stato> neighborhood = new ArrayList<>();
         //Usiamo un Set di Stringhe per tracciare gli ID presi

        Card card = stato.getCards().get(pos);

        // 1. Gestione Carte Obbligatorie
        // Supponendo che la carta alla posizione pos sia una delle carte volute dal player
        //allora non sarà possibile spostarsi dallo stato corrente
        List<String> mandatoryCardsId = vincoli.mandatoryCardsId;
        if(mandatoryCardsId!=null && !mandatoryCardsId.isEmpty()) {
            for (String c : mandatoryCardsId) {
                if (card.getId().equals(c)) {
                    Stato stato1 = new Stato(stato.getCards());
                    stato1.setUtility(funz.Totale_FU(stato1));
                    neighborhood.add(stato1);
                    return neighborhood;
                }
            }
        }


            for(int n=0,tentativi=0;tentativi<=numTry && n<qty;) {
                //definisco la lista dei vicini generabili a partire dalla sostituzione di un dato alla posizione pos
                List<Card> newSet  = new ArrayList<>();
                Card c=pool.get(rand.nextInt(pool.size()));
                if(stato.getCards().contains(c) && !card.getId().equals(c.getId())) {
                    tentativi++;
                    continue;
                }
                for(int count=0;count<8;count++) {
                    if(count == pos)
                        newSet.add(c);
                    else
                        newSet.add(stato.getCards().get(count));
                }
                if(checkConstraints(newSet,vincoli)) {
                    Stato stato1 = new Stato(newSet);
                    stato1.setUtility(funz.Totale_FU(stato1));
                    neighborhood.add(stato1);
                    n++;
                }else{
                    tentativi++;
                }
            }

        return neighborhood;
    }


    /**
     * Verifica i vincoli usando la logica dei bucket fornita.
     */
    private boolean checkConstraints(List<Card> cards, Vincoli vincoli) {

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
        if (vincoli.nSpells != null && countSpells != vincoli.nSpells) return false;
        if (vincoli.nBuildings != null && countBuildings != vincoli.nBuildings) return false;
        if (vincoli.nFlyingTroop != null && countFlying != vincoli.nFlyingTroop) return false;
        if (vincoli.nBuildingTarget != null && countBuildingTarget != vincoli.nBuildingTarget) return false;

        return true;
    }
}
