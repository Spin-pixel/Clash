package main.java.org.ProgettoFIA.gacore.fitness;

import main.java.model.*;
import main.java.model.Card.*;
import main.java.org.ProgettoFIA.gacore.individuals.Deck;
import main.java.service.CardService;

import java.util.List;

public class FitnessFunction {

    //prendo la lista delle carte per non chiamare ogni volta
    private static final CardService cardService = new CardService();
    private static final List<Card> allCards = cardService.loadCards("GA-Clash-core/src/cardList.json");

    private static final double Delta = 1;
    private static final double T = 3.6;

    // Idea: scegliere valori che rappresentano "copertura buona" (es. percentile 75/85 su deck casuali)
    private static final double TAU_AOE = 3200; // soglia per AoE power
    private static final double TAU_DPS = 850;  // soglia per top1+top2 DPS
    private static final double TAU_HP = 400;

    public double FinalFitness(Deck deck) {
        return (0.55 * ElisirFitness(deck)) + (0.45 * CoverageFitness(deck));
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
        double value = 1 - (Math.abs(avg - T) / Delta);
        return Math.max(0.0, value);
    }

    public double CurveElisirFitness(int low, int high){
        double value = 0;
        if(low < 2) value += 2 - low;
        if(low > 4) value += low - 4;
        if(high > 2) value += high - 2;
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

        //Calcolo delle varie componenti diverso per ogni tipo di carta
        for (Card card : cards) {

            if (card instanceof Troop){
                Troop troop = (Troop) card;
                //le truppe possono essere composte da gruppi, percio va considerato ma per non dare
                //troppo peso ai gruppi li elevo a un valore compreso tra 0 e 1 e introduco un fattore pesato sulla vita della singola truppa
                double wSize = 1;
                if(troop.getGroupSize() > 1){
                    wSize = Math.pow(troop.getGroupSize(), 0.6);
                }

                //calcolo dell dps pesando numero truppe e punti vita
                double effectiveDPS = wSize * (troop.getDamage() / troop.getHitSpeed()) * (0.5 + 0.5 * clamp01(troop.getHitpoints() / TAU_HP));
                if(troop.getAttackScope() == AttackScope.AIR_GROUND){
                    nAir++;
                }

                if(troop.getRadius() > 0.0){
                    sumAoePower += effectiveDPS;
                    hasAnyAoe = true;
                }

                if (effectiveDPS > top1Dps){
                    top1Dps = effectiveDPS;
                } else if (effectiveDPS < top2Dps) {
                    top2Dps = effectiveDPS;
                }
            }

            //le strutture non hanno bisogno di fattori complicati essendo singole, percio dps effettivo e calcoli semplici
            if (card instanceof DefensiveBuilding defensiveBuilding){
                if(defensiveBuilding.getAttackScope() == AttackScope.AIR_GROUND){
                    nAir++;
                }
                if(defensiveBuilding.getRadius() > 0.0){
                    sumAoePower += (defensiveBuilding.getDamage()/ defensiveBuilding.getHitSpeed());
                    hasAnyAoe = true;
                }
                if ((defensiveBuilding.getDamage()/ defensiveBuilding.getHitSpeed()) > top1Dps){
                    top1Dps = (defensiveBuilding.getDamage() / defensiveBuilding.getHitSpeed());
                } else if ((defensiveBuilding.getDamage() / defensiveBuilding.getHitSpeed()) < top2Dps) {
                    top2Dps = (defensiveBuilding.getDamage() / defensiveBuilding.getHitSpeed());
                }
            }

            if (card instanceof SpawnerTroop){
                //recupero la spawner troop e la spawned troop
                SpawnerTroop spawnerTroop = (SpawnerTroop) card;
                Troop spawnedTroop = (Troop) allCards.stream()
                        .filter(c -> spawnerTroop.getIdSpawnedTroop().equals(c.getId())) //trova la carta tramite l'id preso dalla spawner card
                        .findFirst()                        // Prendi il primo risultato
                        .orElse(null);                      // Restituisci null se non la trovi

                double spawnerDps = spawnerTroop.getDamage() / spawnerTroop.getHitSpeed();
                //calcolo velocità di spawn della truppa tenendo conto quante truppe spawna e il tempo di vita medio di una truppa in campo (14 secondi)
                //e poi do lo stesso peso che ho dato per le truppe con gruppi di unità
                double spawnedDps = Math.pow(spawnerTroop.getSpawnCount() * (14 / spawnerTroop.getSpawnInterval()), 0.6) * spawnedTroop.getDamage() / spawnedTroop.getHitSpeed() * (0.5 + 0.5 * clamp01(spawnedTroop.getHitpoints() / TAU_HP));
                double effectiveDps = spawnerDps + Math.pow(spawnedDps, 0.4);

                if (spawnerTroop.getAttackScope() == AttackScope.AIR_GROUND){
                    nAir++;
                }

                 if (spawnerTroop.getRadius() > 0.0 || spawnedTroop.getRadius() > 0.0){
                     sumAoePower += effectiveDps;
                     hasAnyAoe = true;
                 }

                 if(effectiveDps > top1Dps){
                     top1Dps = effectiveDps;
                 }
                 if (effectiveDps > top2Dps) {
                     top2Dps = effectiveDps;
                 }
            }

            if (card instanceof SpawnerBuilding){
                SpawnerBuilding spawnerBuilding = (SpawnerBuilding) card;
                SpawnerTroop spawnerTroop = (SpawnerTroop) card;
                Troop spawnedTroop = (Troop) allCards.stream()
                        .filter(c -> spawnerBuilding.getIdSpawnedTroop().equals(c.getId())) //trova la carta tramite l'id preso dalla spawner card
                        .findFirst()                        // Prendi il primo risultato
                        .orElse(null);
                if (spawnedTroop.getAttackScope() == AttackScope.AIR_GROUND){
                    nAir++;
                }
            }

            //le spell le consideriamo solo nel calcolo dei danni aerei non nel numero di difese aeree
            //ne nel dps per motivi logistici
            if (card instanceof Spell){
                Spell spell = (Spell) card;
                if(spell.getRadius() > 0.0){
                    sumAoePower += spell.getDamage();
                    hasAnyAoe = true;
                }
            }
        }

        // Gate ARIA: vuoi almeno 2 carte AIR_GROUND -> clamp01(nAir/2)
        double gAir = clamp01(nAir / 2.0);

        // Gate AoE: se zero AoE, penalità forte
        double gAoe = hasAnyAoe ? 1.0 : 0.2;

        // Score AoE
        double A = clamp01(sumAoePower / TAU_AOE);

        // Score anti-tank
        double T = clamp01((top1Dps + top2Dps) / TAU_DPS);

        // Combinazione (solo le 2 componenti principali rimaste)
        double core = 0.5 * A + 0.5 * T;

        // Applico i gate
        return gAir * gAoe * core;
    }


    private double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

}
