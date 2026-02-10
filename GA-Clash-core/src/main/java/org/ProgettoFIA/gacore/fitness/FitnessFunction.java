package main.java.org.ProgettoFIA.gacore.fitness;

import main.java.model.Card;
import main.java.org.ProgettoFIA.gacore.individuals.Deck;

import java.util.List;

public class FitnessFunction {

    // Pesi fitness (somma = 1.0)
    private static final double W_ELISIR = 0.55;
    private static final double W_COVERAGE = 0.45;


    // Idea: scegliere valori che rappresentano "copertura buona" (es. percentile 75/85 su deck casuali)
    private static final double TAU_AOE = 1200.0; // soglia per AoE power
    private static final double TAU_DPS = 600.0;  // soglia per top1+top2 DPS

    public double FinalFitness(Deck deck) {
        double fitness = (W_ELISIR * ElisirFitness(deck)) + (W_COVERAGE * CoverageFitness(deck));
        return fitness;
    }

    public double ElisirFitness(Deck deck){
        List<Card> lista = deck.getCards();
        double avg = 0.0;
        int low = 0;
        int high = 0;
        for (Card card : lista) {
            avg += card.getElixirCost();
            switch (card.getElixirCost()) {
                case 1, 2, 3:
                    low++;
                    break;
                case 6, 7, 8, 9:
                    high++;
                    break;

            }
        }
        avg = avg / lista.size();
        return (0.6 * AvgElisirFitness(avg)) + (0.4 * CurveElisirFitness(low, high));
    }

    public double AvgElisirFitness(double avg){
        double value;
        value = 1- (Math.abs(avg - 3.6) / 1);
        if(value < 0){
            return 0;
        }else
            return value;
    }

    public double CurveElisirFitness(int low, int high){
        double value = 0;
        if(low < 2){
            value += 2- low;
        }if(low > 4){
            value += low - 4;
        }
        if(high > 2){
            value += high - 2;
        }
        return 1.0 - Math.min(1.0, value / 8.0);
    }

    public double CoverageFitness(Deck deck){
        List<Card> cards = deck.getCards();

        // 1) Gate ARIA
        int nAir = 0;

        // 2) AoE anti-swarm
        double sumAoePower = 0.0;
        boolean hasAnyAoe = false;

        // 3) Anti-tank: top 2 DPS
        double top1Dps = 0.0;
        double top2Dps = 0.0;

        for (Card card : cards) {
            if (card.)
        }
    }

//    public double SynFitness(Deck deck){
//        return 0;
//    }

//    public double MetaFitness(List<Card> cards){
//        double value = 0.0;
//        for(Card card : cards){
//            //value += 0.6 * card.getWinRate() + 0.4 * card.getUsage();
//        }
//        return value / 8;
//    }
}
