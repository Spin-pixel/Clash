package grafica;

import java.util.Locale;
import java.util.Map;

/**
 * Converte l'id delle carte del JSON nel path dell'immagine presente in resources.
 *
 * Regola base: id (snake_case) -> filename (kebab-case) + ".png"
 * Esempio: "hog_rider" -> "/img/immagini carte/hog-rider.png"
 *
 * Eccezioni gestite in OVERRIDES.
 */
public final class CardImageResolver {

    private static final String BASE = "/img/immagini carte/";

    // id -> filename reale in resources
    private static final Map<String, String> OVERRIDES = Map.of(
            "snowball", "giant-snowball.png"
    );

    private CardImageResolver() {}

    public static String resolve(String cardId) {
        if (cardId == null || cardId.isBlank()) return null;

        String id = cardId.trim().toLowerCase(Locale.ROOT);

        String overridden = OVERRIDES.get(id);
        String filename = (overridden != null)
                ? overridden
                : id.replace('_', '-') + ".png";

        return BASE + filename;
    }
}