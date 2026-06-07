# Barbu

Barbu ist ein französisches Kartenspiel für vier Spieler. 
Es gehört zur Familie der Compendium-Spiele: 
Eine vollständige Partie ist in eine Reihe unabhängiger Teilspiele unterteilt,
von denen jedes seine eigenen Regeln hat.
Jeder Spieler ist nacheinander Declarer und spielt dabei eines seiner verbleibenden Teilspiele (Contracts) nach dem anderen, bis keines mehr übrig ist. 
Danach geht die Rolle an den nächsten Spieler über.


Für Barbu gibt es kein einheitliches offizielles Regelwerk. 
Die konkrete Auswahl der Contracts, ihre Wertungssysteme und weitere Regeldetails wie die Anzahl der Karten im Deck unterscheiden sich von Region zu Region und von Haushalt zu Haushalt.
In den meisten Varianten sind die meisten Contracts Stichspiele mit unterschiedlichen Wertungszielen. 
Die meisten Varianten enthalten außerdem ein oder zwei Contracts, die keine Stichspiele sind, am häufigsten Réussite (auch als Domino bekannt):
ein Spiel im Patience-Stil, bei dem die Spieler versuchen, als Erste alle Handkarten loszuwerden, indem sie diese auf vier Farbstapel auf einem gemeinsamen Tableau ablegen.

Ziel dieses Projekts war es, die Barbu-Variante zu implementieren, die ich kenne und regelmäßig spiele, 
und dabei den Code so zu gestalten, dass er sich um alternative Varianten erweitern lässt.
Dieses Ziel der Erweiterbarkeit hat viele der Designentscheidungen sowohl in der Engine- als auch in der API-Schicht geleitet.

---

## Engine

Die Spiellogik ist als eigenständiges Modul (die Engine) implementiert, mit wenigen externen Abhängigkeiten (Jackson, JSpecify). 
Das Ziel ist Wiederverwendbarkeit und Separation of Concerns: 
Die Engine kann unabhängig von der hier entwickelten konkreten Anwendung genutzt werden.

### Game State

Ein Spiel wird durch den Aufruf von `GameState.newGame(GameSettings, seed)` erzeugt, 
wobei `GameSettings` die vollständige logische Variantendefinition enthält: die Liste der Contracts, die Deckgröße und die Angabe, 
ob die niedrigste oder die höchste Gesamtpunktzahl gewinnt.

Der vollständige Zustand eines Spiels wird durch ein unveränderliches `GameState`-Objekt repräsentiert.
Die Karten werden zufällig, aber reproduzierbar ausgeteilt: Jede Austeilung nutzt ihren Seed, um sie zu mischen und den Seed für die nächste zu erzeugen.

Ein Spiel kann daher aus seinen `GameSettings`, dem initialen Seed und der Abfolge der angewandten Moves rekonstruiert werden.

Es existieren drei konkrete Phasen: `WaitingForContractSelection`, `ContractInProgress` und `GameOver`, 
allesamt Subklassen der abstrakten `GameState`-Klasse.
Die ersten beiden haben mit `ActiveGameState` zusätzlich einen gemeinsamen Supertype, der zwischen ihnen und `GameState` liegt.
Die Hierarchie war zunächst flach, alle drei erbten direkt von `GameState`. 
Die beiden aktiven States teilen jedoch Verhalten (wie `legalMoves()`, `currentPlayer()` und `applyMove()`), das auf `GameOver` nicht zutrifft.
Mit `ActiveGameState` setzt das Type-System diese Unterscheidung bereits zur Compile-Zeit durch, sodass wiederholte Runtime-Checks entfallen.


### Contracts 

`GameSettings` enthält die Liste der für ein Spiel verfügbaren Contracts. 
Wählt der Declarer einen Contract aus, ruft der aktive `WaitingForContractSelection`-State `start` auf dem gewählten `Contract` auf 
und erhält ein `ContractState`-Objekt, ohne den konkreten Contract-Typ kennen zu müssen. 
Der daraus erzeugte `ContractInProgress`-State kapselt dieses `ContractState`-Objekt und delegiert `legalMoves()`, `currentPlayer()` und `applyMove()` an es. 
So muss `GameState` die Spiellogik der einzelnen Contracts nicht kennen.

Die Engine implementiert zwei Contract-Typen: `TrickTakingContract` und `ReussiteContract`.

`TrickTakingContract` ist durch zwei Strategy-Objekte parametrisiert: 
* `ScoringPolicy`: bestimmt, wie Punkte vergeben werden
* `LeadRestriction`: bestimmt, welche Karten einen Stich anspielen dürfen

Beide sind Interfaces mit mehreren Implementierungen sowie einer Composite-Implementierung, die mehrere Instanzen zu einer kombiniert,
sodass sich komplexe Regeln aus einfacheren zusammensetzen lassen.


`ReussiteContract` verfolgt einen anderen Ansatz: Sein Verhalten wird direkt über Record-Parameter konfiguriert.

### Tests

Unit Tests (nicht Teil dieser Abgabe) decken die meisten Komponenten der Engine ab. 
Sie dienten während der Entwicklung zwei Zwecken:
* zu bestätigen, dass jede neue Komponente wie vorgesehen funktionierte, bevor weiterer Code darauf aufbaute
* Regressionen früh zu erkennen, wenn bestehender Code geändert oder erweitert wurde

---

## Bots

Alle Bots erweitern eine abstrakte `Bot`-Klasse. Ihre öffentliche Methode `chooseMove(ActiveGameState)` 
prüft zunächst, ob nur ein einziger legaler Move existiert, und gibt ihn in diesem Fall sofort zurück. 
Andernfalls delegiert sie an `search()`, das jeder konkrete `Bot` implementiert.

Die Engine stellt mehrere Bot-Implementierungen bereit. Dazu gehören:
* `RandomMoveBot` (wählt einen zufälligen legalen Move),
* `MctsBot` (Monte-Carlo Tree Search) und
* `IsmctsBot` (Information-Set Monte-Carlo Tree Search).

### Monte Carlo Tree Search

Der Monte Carlo Tree Search (MCTS) Algorithmus wurde aus drei Hauptgründen gewählt, um stärkere Bots zu implementieren:
- Er benötigt kein Domänenwissen über das Spiel.
- Die Schwierigkeit lässt sich leicht über die Anzahl der Iterationen anpassen.
- Er erzeugt abwechslungsreicheres Spiel als eine feste Heuristik.

MCTS funktioniert, indem es schrittweise einen Search Tree aufbaut. Jede Iteration besteht aus vier Schritten: 
1. **Selektion** eines Pfads durch den Tree bis zu einem Leaf Node,
2. **Expansion** des Trees durch Hinzufügen eines neuen Nodes für einen unerkundeten Move,
3. **Simulation** des Spiels ab diesem Node mithilfe einer schnellen Policy und schließlich
4. **Backpropagation** des Ergebnisses entlang des gewählten Pfads.

Die Node-Selektion verwendet die Upper Confidence Bound for Trees (UCT) Formel:

$$UCT(i) = \frac{w_i}{n_i} + C \cdot \sqrt{\frac{\ln N_i}{n_i}}$$

wobei
- $w_i$: Anzahl der an Node $i$ gesammelten Siege
- $n_i$: Anzahl der Besuche von Node $i$
- $N_i$: Anzahl der Besuche seines Parents
- $C$: Exploration-Konstante

Der erste Term ist der **Exploitation**-Term; er bevorzugt Nodes, die bisher gute Ergebnisse geliefert haben. 
Der zweite Term ist der **Exploration**-Term; er bevorzugt Nodes, die relativ zu ihrem Parent seltener besucht wurden.

Die Exploration-Konstante $C$ steuert die Balance zwischen beiden Termen.

### Für Barbu

#### Scoring
Barbu ist kein reines Win-or-lose-Spiel: Das Ergebnis ergibt sich aus den kumulativen Scores über alle Contracts, sodass die Siegzahl $w_i$ nicht direkt anwendbar ist. Stattdessen wird jede Simulation über ihren Margin bewertet:
- Gewinnt der höchste Score: eigener Score minus dem höchsten gegnerischen Score
- Gewinnt der niedrigste Score: niedrigster gegnerischer Score minus eigenem Score

Ein positiver Margin bedeutet, dass der Bot aktuell in Führung liegt. Dieser Margin ist der Wert, der im Tree zurückpropagiert wird; $w_i$ sammelt somit Margins statt Siege, und $w_i / n_i$ entspricht dem durchschnittlichen Margin des Nodes. Damit die Exploration-Konstante $C$ über Contracts mit sehr unterschiedlichen Punktebereichen ausgewogen bleibt, wird dieser durchschnittliche Margin gegen den niedrigsten und höchsten während der Suche beobachteten Margin auf $[0,1]$ normalisiert. Dieser normalisierte Wert dient als Exploitation-Term in der UCT-Formel.

#### Simulation depth
Simulationen enden am Ende des aktuellen Contracts und nicht am Ende des Spiels. Die Hände für jeden Contract werden unabhängig ausgeteilt, sodass die erwarteten Margin-Beiträge zukünftiger Contracts über alle Kandidaten-Moves hinweg identisch sind; sie heben sich beim Vergleich der Optionen auf. Das gilt sowohl für Moves innerhalb eines Contracts als auch für die Contract-Selektion.

#### Imperfect information
MCTS setzt einen vollständig bekannten Game State voraus, doch in Barbu sind die Hände der Gegner verdeckt. Eine Determinization ersetzt die unbekannten gegnerischen Hände durch ein plausibles Sample und erzeugt so einen vollständig bekannten State, den der Algorithmus durchsuchen kann. Der `Determinizer` verteilt die verbleibenden Karten auf die Gegner anhand der dem Bot verfügbaren Informationen: seiner eigenen Hand, der bereits gespielten Karten und der `impossibleCards`-Constraints, die von der Engine verfolgt werden.

Die beiden MCTS-Implementierungen unterscheiden sich vor allem im Umgang mit dieser Determinization:
* `MctsBot` baut pro Determinization einen eigenen Search Tree und führt sie parallel aus. Jeder Tree stimmt für seinen besten Move; der Move mit den meisten Stimmen gewinnt.
* `IsmctsBot` hält stattdessen einen einzigen gemeinsamen Tree über alle Determinizations hinweg und zieht zu Beginn jeder Iteration eine neue Determinization. Da manche Nodes nur unter bestimmten Determinizations erreichbar sind, unterrepräsentiert ihr Visit Count, wie oft sie hätten gewählt werden können. Um das zu berücksichtigen, verfolgt jeder Node `timesAvailable` (die Anzahl der Iterationen, in denen er erreichbar war), und die UCT-Formel verwendet diesen Count anstelle des Parent Visit Counts.

### Tests

Um die Spielstärke der Bots zu bewerten und Konfigurationsparameter abzustimmen, wurde ein `mcts-simulations`-Modul (nicht Teil dieser Abgabe) entwickelt, 
das Bot-gegen-Bot-Spiele ausführt und die Ergebnisse zur Auswertung als CSV-Dateien schreibt. 
Die Benchmarks verglichen MCTS- und IS-MCTS-Bots über verschiedene Iterationsbudgets hinweg und maßen ihre Leistung sowohl gegen `RandomMoveBot`
als auch gegen `HeuristicBot`-Gegner.

Der `HeuristicBot` (ebenfalls nicht Teil dieser Abgabe) ist ein regelbasierter Gegner: 
Statt zu suchen, wählt er seine Moves anhand fester Regeln. 
Er ist deutlich stärker als zufälliges Spiel und gewinnt in der Standard-Barbu-Variante rund 80 % der Spiele gegen drei `RandomMoveBot`-Gegner.
Er wurde entwickelt, um zu beurteilen, wie gut die MCTS-Bots gegen stärkere Gegner abschneiden.

Diese Ergebnisse bildeten die empirische Grundlage für die Wahl der Iterationszahlen und der Exploration-Konstante für die in der Anwendung verwendeten Bots der Schwierigkeitsstufen Medium und Hard.

---

## API

Die API-Schicht wurde bereits von der Gruppe dokumentiert. 
Dieser Abschnitt behandelt ausgewählte Designentscheidungen, 
die dort nicht angesprochen werden.

### Zustandsübergänge und das Frontend

Wenn die letzte Karte eines Stichs gespielt wird, 
treibt die Engine den State intern voran: 
Der Stich wird verarbeitet und der State bewegt sich weiter zu einem leeren Stich oder zur nächsten Phase. 
Der abgeschlossene Stich ist nicht Teil des resultierenden States, 
doch das Frontend benötigt ihn für die Anzeige. 
Dasselbe gilt für das finale Tableau am Ende eines Réussite-Contracts.

Die Lösung ist auf API-Ebene umgesetzt: Nach jedem Move prüft der Server, 
ob ein Stich oder ein Tableau gerade abgeschlossen wurde, 
rekonstruiert ihn aus dem vorherigen State und dem Move und nimmt ihn in das `PublicGameUpdateDTO` auf.

### Datenmodell

In der Datenbank wird kein mutable Game State gespeichert. 
Ein Spiel wird als seine initialen `GameSettings`, 
sein Seed und ein Append-only-Log von Moves als `GameMoveEntity`-Zeilen persistiert. 
Wenn der aktuelle State benötigt wird, rekonstruiert ihn `GameState.replay(settings, seed, moves)` aus diesen.

`PlayerEntity` verwendet JPA Joined Inheritance: 
Gemeinsame Felder liegen in einer geteilten `game_players`-Tabelle. 
`HumanPlayerEntity` fügt eine Zeile in `human_players` mit einem foreign key zum Benutzerkonto hinzu. 
`BotPlayerEntity` fügt eine Zeile in `bot_players` mit dem Bot-Typ und dem Display Name hinzu.

### Barbu Catalog

Der `GameCatalog` definiert die Barbu-Varianten der Anwendung, jede als `GameVariant`. 
Eine `GameVariant` ist das Gegenstück des Katalogs zu `GameSettings` der Engine und ergänzt dieses um eine stabile ID und einen menschenlesbaren Namen. Ihre Contracts werden analog behandelt: Jede `ContractDefinition` verknüpft einen Engine-`Contract` mit eigener ID und Anzeigename.

Eine neue Variante hinzuzufügen erfordert lediglich eine neue `GameVariant`-Definition in `GameCatalog`; API oder Frontend müssen nicht angepasst werden.

Ursprünglich speicherte `GameEntity` die Variant-ID als Verweis auf den Catalog – ein De-facto-Fremdschlüssel ohne Datenbank-Enforcement, der beim Umbenennen oder Entfernen einer Variante bestehende Spiele stillschweigend hätte beschädigen können. Stattdessen speichert `GameEntity` nun einen vollständigen Snapshot der Varianteninformationen direkt. Bei der Erstellung wird für jeden Contract eine `GameContractEntity`-Zeile angelegt, die per echtem Fremdschlüssel mit der `GameEntity` verknüpft ist. Ein Spiel ist dadurch in sich abgeschlossen und hängt nicht mehr vom unveränderten Catalog ab.

`GameCatalog` wurde später in ein eigenes `barbu-catalog`-Modul extrahiert, damit es auch vom `mcts-simulations`-Modul genutzt werden konnte, ohne eine Abhängigkeit von Spring einzuführen.