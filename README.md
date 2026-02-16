# Royal Configurator

Repository: https://github.com/Spin-pixel/Clash

Royal Configurator è un software che supporta i giocatori di **Clash Royale** nella creazione di mazzi efficaci.  
L’obiettivo è fornire uno strumento soprattutto ai **giocatori novizi**, così da ottenere deck solidi anche con poca esperienza, tramite strategie di ottimizzazione basate su **euristiche** e **meta-euristiche**.

---

## Team

| Membro | Contributo |
|---|---|
| Francesco Maggio | Euristiche matematiche, definizione della classe **Fitness** e definizione delle metriche |
| Marcello Lettieri | Sviluppo dell’**Algoritmo Genetico (GA)**, lavoro di **Data Understanding** |
| Igino Alessandro Iannotta | **GUI JavaFX** e **Simulated Annealing (SA)** |

---

## Obiettivo del progetto

- Generare mazzi (deck) a partire da un pool di carte.
- Confrontare strategie diverse di ottimizzazione:
  - **Genetic Algorithm (GA)**
  - **Simulated Annealing (SA)**
- Mostrare i risultati tramite una **GUI JavaFX**.
- Valutare e confrontare gli algoritmi tramite **metriche di analisi**.

---

## Struttura del progetto

Il progetto segue una struttura **Maven standard**, con `main` e `test` separati (nota: `test` è fuori da `main`).

```text
src/
 ├─ main/
 │   ├─ java/
 │   │   ├─ agente/
 │   │   │   ├─ Genetic_Algoritm/        (Algoritmo Genetico - GA)
 │   │   │   └─ Simulated_Annealing/     (Simulated Annealing - SA)
 │   │   ├─ grafica/                     (GUI JavaFX)
 │   │   ├─ model/                       (dominio: carte, deck, vincoli, ecc.)
 │   │   ├─ service/                     (supporto: parsing/mapping)
 │   │   └─ metriche/                    (metriche di valutazione)
 │   └─ resources/
 │       ├─ *.fxml                       (layout JavaFX)
 │       ├─ *.css                        (stili)
 │       └─ img/                         (immagini, es. carte)
 └─ test/
     └─ java/                            (test e sperimentazioni locali)


---

## Dataset (JSON interno)

Non vengono utilizzati dataset esterni: il sistema usa un dataset “artificiale” interno, descritto in un file JSON incluso nel repository.

- File JSON: `src/main/java/cardList.json`

> Nota tecnica: questo path non è quello tipico (`src/main/resources`).  
> Il progetto funziona comunque se il caricamento è fatto dal filesystem/relativo, ma per replicare fedelmente è importante **non spostare** il file e mantenere la stessa struttura.

---

## Come replicare il progetto (IntelliJ + Maven + JavaFX)

### Requisiti
- **IntelliJ IDEA**
- **Java SDK 23** configurato come Project SDK
- **Maven** (gestito da IntelliJ o da CLI)
- Modulo **JavaFX** (il progetto è sviluppato come applicazione JavaFX)

### Branch consigliato
⚠️ **Usare il branch `Integrated`**.

Durante lo sviluppo sono emersi problemi legati alla gestione dei moduli; per questo motivo si è reso necessario **ricopiare tutte le classi in un nuovo progetto IntelliJ**, pubblicato nel branch `Integrated`.  
È quindi **sconsigliato** scaricare/aprire altri branch perché potrebbero non essere coerenti o non eseguibili.

### Istruzioni (da IntelliJ)
1. Clona il repository e fai checkout del branch:
  - `Integrated`
2. Apri il progetto in IntelliJ (**Open** sulla root del repository).
3. Imposta **Project SDK = Java 23**:
  - `File > Project Structure > Project > SDK`
4. Assicurati che Maven sia correttamente importato (pom.xml rilevato).
5. Esegui la GUI avviando la classe:
  - `src/main/java/grafica/MainApp.java`

---

## Avvio rapido

- **GUI**: avvia `MainApp`  
  Percorso: `src/main/java/grafica/MainApp.java`

---

## Note

- Per una replica fedele, mantenere invariati:
  - struttura cartelle
  - file JSON interno (`src/main/java/cardList.json`)
  - risorse in `src/main/resources` (FXML/CSS)
  - immagini in `src/main/resources/img` (PNG)
