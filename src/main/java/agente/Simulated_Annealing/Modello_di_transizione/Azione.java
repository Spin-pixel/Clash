package agente.Simulated_Annealing.Modello_di_transizione;

import agente.Simulated_Annealing.Stato_corrente.Stato;
import model.Card;

import java.util.List;

public class Azione {
    public Stato replace(List<Card> pool,int pos){
        return new Stato(pool);
    }
}
