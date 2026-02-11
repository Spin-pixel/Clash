import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;
import main.java.org.ProgettoFIA.gacore.initializer.Initializer;
import main.java.org.ProgettoFIA.gacore.operatori_genetici.Crossover;
import main.java.service.CardService;
import main.java.model.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---\n" +
                            "        [CROSSOVER DEMO]        \n\n");

        CardService service = new CardService();

        final String path = "GA-Clash-core/src/cardList.json";
        // 1. Carica le carte (usa il tuo CardService fatto prima)
        CardService cardService = new CardService();

        DeckConstraints constraints = new DeckConstraints();
        constraints.nFlyingTroop=3;
        constraints.nSpells=1;
        constraints.nBuildings=1;
        constraints.nBuildingTarget=1;
        constraints.mandatoryCardsId=new ArrayList<>();
        constraints.mandatoryCardsId.add("bats");


        List<Card> allCards = cardService.loadCards(path);
        Initializer initializer = new Initializer();
        List<Deck> deckList = initializer.createPopulation(allCards,20, constraints);
        Crossover crossover=new Crossover();
        Crossover.setLogging(true);
        for (int i=1; i<deckList.size();i++){
            Deck dad = deckList.get(i-1);
            Deck mom = deckList.get(i);

            System.out.print("PARENT_N."+(i-1)+" [ ");
            for(Card c: dad.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n");

            System.out.print("PARENT_N."+(i)+" [ ");
            for(Card c: mom.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n");

            Deck son = (crossover.performCrossover(dad,mom,constraints));

            System.out.print("SON [ ");
            for(Card c: son.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n---------------------------------------------\n");


        }

    }
}