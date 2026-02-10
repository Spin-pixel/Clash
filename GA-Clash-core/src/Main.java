import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;
import main.java.org.ProgettoFIA.gacore.initializer.Initializer;
import main.java.service.CardService;
import main.java.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---");

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
        constraints.mandatoryCardsId.add("lightning");
        constraints.mandatoryCardsId.add("battle_ram");
        constraints.mandatoryCardsId.add("baby_dragon");


        List<Card> allCards = cardService.loadCards(path);
        Initializer initializer = new Initializer();
        List<Deck> deckList = initializer.createPopulation(allCards,20, constraints);

        for (Deck d: deckList){
            System.out.print("DECK [ ");
            for(Card c: d.getCards())
                System.out.print(c.getName() + ",");
            System.out.print(" ]\n");
        }
    }
}