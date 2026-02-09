import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;
import main.java.org.ProgettoFIA.gacore.initializer.Initializer;
import main.java.service.CardService;
import main.java.model.*;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---");

        CardService service = new CardService();

        final String path = "GA-Clash-core/src/cardList.json";
        // 1. Carica le carte (usa il tuo CardService fatto prima)
        CardService cardService = new CardService();
        List<Card> allCards = cardService.loadCards(path);
        System.out.println("CARTE LETTE: " + allCards.size());

        for (Card c:allCards){
            System.out.println(c);
        }
    }
}