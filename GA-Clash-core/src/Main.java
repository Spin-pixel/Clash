import main.java.service.CardService;
import main.java.model.*;

import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- Clash Royale Card Loader ---");

        CardService service = new CardService();

        final String path = "GA-Clash-core/src/cardList.json";
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        // Assicurati che il path sia corretto rispetto alla root del tuo progetto
        // In IntelliJ solitamente la root è fuori da 'src', quindi: "src/cardList.json"

        List<Card> deck = service.loadCards(path);

        System.out.println("Carte caricate con successo: " + deck.size());
        System.out.println("-------------------------------------------------");

        for (Card card : deck) {
            // Usa il polimorfismo: Java chiamerà il toString() della classe specifica
            System.out.println(card);

            // Esempio di calcolo efficienza
            System.out.printf("   -> Efficiency Score: %.2f%n", card.getEfficiencyScore());
        }
    }
}