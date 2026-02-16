# Royal Configurator

Royal Configurator è un software che ha l’obiettivo di supportare i giocatori di **Clash Royale** nella creazione di mazzi efficaci.

L’idea è offrire uno strumento soprattutto ai **giocatori novizi**, così da ottenere **deck solidi** anche in assenza di esperienza, tramite tecniche di ottimizzazione basate su strategie euristiche e meta-euristiche.

---

## Obiettivi

- Supportare la costruzione di un mazzo a partire da un pool di carte disponibile.
- Generare soluzioni tramite strategie di ricerca/ottimizzazione.
- Mostrare i risultati in modo chiaro attraverso una **GUI JavaFX**.

---

## Struttura del progetto

Il progetto è organizzato in **2 macromoduli**:

### 1) `JAVA`
Contiene la logica applicativa e l’implementazione delle strategie.

- `agente/`  
  Contiene le strategie utilizzate per risolvere il problema:
    - `Genetic_Algoritm/` — Algoritmo Genetico (GA)
    - `Simulated_Annealing/` — Simulated Annealing (SA)

- `grafica/`  
  Controller e classi di supporto per la GUI **JavaFX**.

- `model/`  
  Classi che modellano le entità del dominio (carte, deck, vincoli, ecc.) e gli oggetti usati per rappresentare gli individui.

- `service/`  
  Classi per il caricamento/mapping dei dati (es. parsing del JSON) verso le classi definite nel `model`.

### 2) `resources`
Contiene le risorse che definiscono l’aspetto del sistema e alimentano l’interfaccia grafica.

- Risorse **statiche e dinamiche** (es. CSS, immagini, file FXML, ecc.) utilizzate dalla GUI.

---

## Divisione dei compiti

Si ringrazia:

- **Francesco Maggio** — euristiche matematiche e definizione della classe **Fitness**
- **Marcello Lettieri** — sviluppo dell’**Algoritmo Genetico**
- **Igino Alessandro Iannotta** — **grafica JavaFX** e **Simulated Annealing**

---

## Note

Le soluzioni generate dipendono dai vincoli impostati, dal pool/dataset di carte disponibile e dalle euristiche adottate.
