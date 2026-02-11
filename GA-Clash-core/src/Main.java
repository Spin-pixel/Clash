import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;
import main.java.org.ProgettoFIA.gacore.initializer.Initializer;
import main.java.org.ProgettoFIA.gacore.operatori_genetici.Crossover;
import main.java.org.ProgettoFIA.gacore.operatori_genetici.Mutation;
import main.java.service.CardService;
import main.java.model.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---\n" +
                            "        [CROSSOVER DEMO]        \n\n");

        CardService service = new CardService();

        // 1. Carica le carte (usa il tuo CardService fatto prima)
        final String path = "GA-Clash-core/src/cardList.json";
        CardService cardService = new CardService();
        List<Card> allCards = cardService.loadCards(path);

        DeckConstraints constraints = new DeckConstraints();
        constraints.nFlyingTroop=3;
        constraints.nSpells=1;
        constraints.nBuildings=1;
        constraints.nBuildingTarget=1;
        constraints.mandatoryCardsId=new ArrayList<>();
        constraints.mandatoryCardsId.add("bats");


        Initializer initializer = new Initializer();
        List<Deck> deckList = initializer.createPopulation(allCards,4, constraints);
        Crossover crossover=new Crossover();
        Crossover.setLogging(false);

        System.out.println("[1° GEN]");
        for (Deck d:deckList){
            System.out.print("DECK [ ");
            for(Card c: d.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n");
        }

//        int nNewGen = 3;
//        for(int i=0;i<1;i++){
//            deckList=crossover.newGeneration(deckList,5,constraints);
//            System.out.println("["+(i+2)+"° GEN]");
//            for (Deck d:deckList){
//                System.out.print("DECK [ ");
//                for(Card c: d.getCards())
//                    System.out.print(c.getName() + ",");
//                System.out.print(" ]\n");
//            }
//        }

        Mutation mutation=new Mutation(allCards);
        mutation.mutateGeneration(deckList,0.5,2,constraints);

        System.out.println("[MUTATION]");
        for (Deck d:deckList){
            System.out.print("DECK [ ");
            for(Card c: d.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n");
        }
    }
}