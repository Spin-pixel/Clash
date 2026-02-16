package agente.Simulated_Annealing;


import agente.Simulated_Annealing.Modello_di_transizione.Azione;
import agente.Simulated_Annealing.Stato_corrente.Stato;
import agente.Simulated_Annealing.Stato_corrente.Vincoli;
import agente.Simulated_Annealing.Stato_corrente.stato_iniziale;
import model.Card;
import model.Troop;

import java.util.*;

public class SA_core {
    public record Params(
            int numeroTry,  //n. di tentativi in cui si ripete la generazione dello stato per oviare il fallimento
            int numCarte,    //definisce il numero di carte che provo a sostituire al più per cella dello stato
            double tempIniziale, // definisce la temperatura iniziale
            double tempFin, // definisce la temperatura finale
            double raff //definisce il tasso di raffreddamento
    ) {
        public static Params defaults(){
            return new Params(
                    500,
                    101,
                    10000,
                    1,
                    0.95
            );
        }
    }
    public record Output(Stato bestStato, String details, String log) {
    }

    public static Output run(List<Card> pool,
                                     Vincoli vincoli,
                                     double delta,
                                     double desiredAvgElixir
    ) {
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(vincoli, "constraints");

        StringBuilder log = new StringBuilder();

        if (pool.size() < Stato.SIZE_STATO) {
            String msg = "Pool carte troppo piccolo: servono almeno 8 carte, ne hai " + pool.size();
            return new Output(null, msg, msg);
        }

        //inizializzo le risorse necessarie a runnare l'algoritmo
        stato_iniziale starter=new stato_iniziale();
        Azione movment =new Azione();

        //istanzio la prima soluzione
        Stato start=starter.createStato(pool,vincoli,Params.defaults().numeroTry(),delta,desiredAvgElixir);


        List<Stato> cards=movment.findNeighborhood(pool,start,vincoli,Params.defaults().numCarte,Params.defaults().numeroTry,delta,desiredAvgElixir);
        Collections.sort(cards);
        cards =cards.reversed();
        Stato best=cards.get(0);


        //setto il tempo dell'algoritmo
        double time = Params.defaults().tempIniziale;

        time*=(1- Params.defaults().raff);

        //ciclo di raffreddamento
        while (time>Params.defaults().tempFin) {
            //genero l'insieme delle soluzioni vicine
            cards=movment.findNeighborhood(pool,best,vincoli,Params.defaults().numCarte,Params.defaults().numeroTry,delta,desiredAvgElixir);
            Collections.sort(cards);

            //scelgo la soluzione più conveniente
            cards =cards.reversed();
            Stato newbest=cards.get(0);

            //scalgo se accettare la soluzione
            if(acceptanceProbability(newbest.getUtility(),best.getUtility(),time)>Math.random())
                best=newbest;

            //raffreddamento
            time*=(1- Params.defaults().raff);
        }

        return new Output(best,formatDetails(best,vincoli,delta),"");
    }

    /**
     * Metodo che calcola la probabilità di accettazione
     * */
    private static double acceptanceProbability(double currentEnergy, double neighborEnergy, double temp) {
        if (neighborEnergy < currentEnergy) {
            return 1.0; // Accetta sempre se migliore
        }
        // Accetta se peggiore con probabilità basata sulla temperatura
        return Math.exp((currentEnergy - neighborEnergy) / temp);
    }



    /**
     * Metodo per formattare i dati del deck vincente
     * */
    private static String formatDetails(Stato best,
                                        Vincoli c,
                                        double delta
    ) {
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
        sb.append("Fitness: ").append(fmt(best.getUtility())).append("\n");
        sb.append("Delta: ").append(fmt(delta)).append("\n");
        /**
         * si può aggiungere il valore dell'euristica
         * */

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
