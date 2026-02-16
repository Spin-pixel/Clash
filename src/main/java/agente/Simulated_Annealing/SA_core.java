package agente.Simulated_Annealing;

public class SA_core {
    public record Params(
            int numeroTry,  //n. di tentativi in cui si ripete la generazione dello stato per oviare il fallimento
            int numCarte    //definisce il numero di carte che provo a sostituire al più per cella dello stato
    ) {
        public static Params defaults(){
            return new Params(
                    30,
                    101
            );
        }
    }
}
