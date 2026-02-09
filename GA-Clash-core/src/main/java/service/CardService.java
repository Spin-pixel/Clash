package main.java.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.model.*;
import main.java.model.Card.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CardService {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Card> loadCards(String filePath) {
        List<Card> cards = new ArrayList<>();

        try {
            // 1. Legge il file JSON come albero di nodi (Generic Tree)
            JsonNode rootArray = mapper.readTree(new File(filePath));

            if (!rootArray.isArray()) {
                throw new RuntimeException("Il file JSON deve iniziare con un array []");
            }

            // 2. Itera su ogni elemento dell'array
            for (JsonNode node : rootArray) {
                Card card = parseSingleCard(node);
                if (card != null) {
                    cards.add(card);
                }
            }

        } catch (IOException e) {
            System.err.println("Errore durante la lettura del file: " + e.getMessage());
        }

        return cards;
    }

    private Card parseSingleCard(JsonNode node) {
        String classType = node.get("class_type").asText();

        // Dati comuni a tutte le carte
        String id = node.get("id").asText();
        String name = node.get("name").asText();
        int cost = node.get("elixirCost").asInt();
        CardType type = CardType.valueOf(node.get("type").asText());

        switch (classType) {
            case "SPELL":
                return new Spell(id, name, cost,
                        node.get("damage").asInt(),
                        node.get("crownTowerDamage").asInt(),
                        node.get("radius").asDouble());

            case "TROOP":
                return new Troop(id, name, cost,
                        node.get("hitpoints").asInt(),
                        AttackScope.valueOf(node.get("attackScope").asText()),
                        MovementSpeed.valueOf(node.get("movementSpeed").asText()),
                        node.get("damage").asInt(),
                        node.get("hitSpeed").asDouble(),
                        node.get("range").asDouble(),
                        node.get("radius").asDouble(),
                        node.get("targetsOnlyBuildings").asBoolean(),
                        node.get("groupSize").asInt());

            case "DEF_BUILDING":
                return new DefensiveBuilding(id, name, cost,
                        node.get("hitpoints").asInt(),
                        AttackScope.valueOf(node.get("attackScope").asText()),
                        node.get("damage").asInt(),
                        node.get("hitSpeed").asDouble(),
                        node.get("range").asDouble(),
                        node.get("radius").asDouble());

            case "SPAWN_BUILDING":
                return new SpawnerBuilding(id, name, cost,
                        node.get("hitpoints").asInt(),
                        node.get("idSpawnedTroop").asText(),
                        node.get("spawnInterval").asDouble(),
                        node.get("spawnCount").asInt());

            case "SPAWN_TROOP":
                return new SpawnerTroop(id, name, cost,
                        node.get("hitpoints").asInt(),
                        AttackScope.valueOf(node.get("attackScope").asText()),
                        MovementSpeed.valueOf(node.get("movementSpeed").asText()),
                        node.get("damage").asInt(),
                        node.get("hitSpeed").asDouble(),
                        node.get("range").asDouble(),
                        node.get("radius").asDouble(),
                        node.get("targetsOnlyBuildings").asBoolean(),
                        node.get("groupSize").asInt(),
                        node.get("idSpawnedTroop").asText(),
                        node.get("spawnInterval").asDouble(),
                        node.get("spawnCount").asInt());

            default:
                System.out.println("Tipo di carta sconosciuto: " + classType);
                return null;
        }
    }
}