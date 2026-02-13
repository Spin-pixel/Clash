package grafica.storeData;

import model.Card;

import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public final class UserPrefs {

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPrefs.class);
    private static final String KEY_DESELECTED = "deselectedCardIds"; // csv

    private UserPrefs() {}

    public static Set<String> loadDeselectedCardIds() {
        String csv = PREFS.get(KEY_DESELECTED, "");
        if (csv == null || csv.isBlank()) return new HashSet<>();

        Set<String> out = new HashSet<>();
        for (String part : csv.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) out.add(id);
        }
        return out;
    }

    public static void saveDeselectedCardIds(Set<String> deselected) {
        if (deselected == null || deselected.isEmpty()) {
            PREFS.remove(KEY_DESELECTED);
            return;
        }
        String csv = deselected.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(Collectors.joining(","));
        PREFS.put(KEY_DESELECTED, csv);
    }

    /**
     * Owned = tutte le carte - (deselezionate salvate)
     */
    public static Set<String> computeOwnedCardIds(List<Card> allCards) {
        Set<String> deselected = loadDeselectedCardIds();

        Set<String> owned = new HashSet<>();
        if (allCards != null) {
            for (Card c : allCards) {
                if (c != null && c.getId() != null && !c.getId().isBlank()) {
                    owned.add(c.getId().trim());
                }
            }
        }
        owned.removeAll(deselected);
        return owned;
    }
}