import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.org.ProgettoFIA.gacore.individuals.DeckConstraints;
import main.java.org.ProgettoFIA.gacore.initializer.Initializer;
import main.java.service.CardService;
import main.java.model.*;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---");

        CardService service = new CardService();

        final String path = "GA-Clash-core/src/cardList.json";
        // 1. Carica le carte (usa il tuo CardService fatto prima)
        CardService cardService = new CardService();
        List<Card> allCards = cardService.loadCards(path);

        // 2. Definisci i vincoli
        DeckConstraints myConstraints = new DeckConstraints();
        myConstraints.mandatoryCardId = "t09";
        myConstraints.minSpells = 2;
        myConstraints.minAirTarget = 2;
        myConstraints.minBuildingTarget = 1;

        // 3. Inizializza popolazione
        Initializer initializer = new Initializer();
        List<Deck> population = initializer.createPopulation(allCards, 20, myConstraints);

        // Stampa risultato
        System.out.println("Popolazione generata: " + population.size());
        for (Deck d : population) {
            System.out.println(d);
        }
    }
}