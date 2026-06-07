# Barbu

Barbu is a French card game for four players. It belongs to the family of compendium games: 
the full session is divided into a series of independent sub-games, each with its own rules. 
Each player takes a turn as declarer, choosing one of their remaining sub-games (known as contracts) to play until none are left. 
The role then passes to the next player.

Barbu has no single official rulebook. The specific set of contracts, their scoring systems, 
and other rule details such as deck size vary across regions and households. 
The majority of contracts across most variants are trick-taking games with different scoring objectives. 
Most variants also include one or two non-trick-taking contracts, the most common being Réussite (also known as Dominoes), 
a solitaire-style game in which players try to be the first to empty their hand by placing cards on four suit piles on a shared tableau.

The goal of this project was to implement the variant of Barbu I know and regularly play, 
while designing the code to be extensible to alternative variants. 
This goal of extensibility guided many of the design decisions in both the engine and API layers.

---

## Engine

The game logic is implemented as a self-contained module (the engine), with a minimal set of external dependencies (Jackson, JSpecify). 
The goal is reusability and separation of concerns: the engine can be used independently of the specific application developed here.


### Game State

A game is created by calling `GameState.newGame(GameSettings, seed)`, where `GameSettings` carries the full logical variant definition:
the list of contracts, the deck size, and whether the lowest or highest cumulative score wins.

The complete state of a game is represented by an immutable `GameState` object.
Card deals are random but reproducible, 
since each deal uses its seed to shuffle the cards and to generate the seed for the next deal.
A game can therefore be reconstructed from its `GameSettings`, the initial seed, and the sequence of moves applied.

Three concrete phases exist: `WaitingForContractSelection`, `ContractInProgress`, and `GameOver`, 
all of which are subclasses of the abstract `GameState` class.
The first two also share an intermediate supertype, `ActiveGameState`. 
The hierarchy was initially flat, with all three extending `GameState` directly.
However, the two active states share behaviors (like `legalMoves()`, `currentPlayer()`, and `applyMove()`) that do not apply to `GameOver`. 
Introducing `ActiveGameState` allowed the type system to enforce these distinctions at compile time, 
eliminating the need for repeated runtime checks.

### Contracts

`GameSettings` holds a list of available contracts for the game.
When the declarer selects a contract, the active `WaitingForContractSelection` state calls `start` 
on the chosen `Contract` and receives a `ContractState` object, without needing to know the concrete contract type.
It then creates a new `ContractInProgress` state that wraps this `ContractState` and delegates `legalMoves()`, `currentPlayer()` and `applyMove()` to it. 
This keeps `GameState` agnostic to how any individual contract is actually played.

The engine implements two contract types, `TrickTakingContract` and `ReussiteContract`. 

`TrickTakingContract` is parameterized by two strategy objects: 
* `ScoringPolicy`: Determines how points are awarded
* `LeadRestriction`: Determines what cards may lead a trick

Both are interfaces with multiple implementations, plus a composite implementation that combines multiple instances into one, 
so that complex rules can be expressed by composing simpler ones.

`ReussiteContract` follows a different approach: its behavior is configured directly through record parameters. 

### Testing

Unit tests (not included in this submission) cover most of the components of the engine. 
They served two purposes during development: 
* Confirming that each new component behaved as intended before building further code on top of it
* Catching regressions early when existing code was changed or extended

---

## Bots

All bots extend an abstract `Bot` class. Its public method `chooseMove(ActiveGameState)` 
first checks whether only one legal move exists and returns it immediately if so. 
Otherwise it delegates to `search()`, which each concrete `Bot` implements.

The engine provides multiple bot implementations out of the box. These include: 
* `RandomMoveBot` (selects a random legal move), 
* `MctsBot` (Monte-Carlo Tree Search), and 
* `IsmctsBot` (Information-Set Monte-Carlo Tree Search).

### Monte Carlo Tree Search

The Monte Carlo Tree Search (MCTS) algorithm was chosen to implement stronger bots for three main reasons:
- It requires no domain knowledge of the game
- Difficulty is easily adjustable by changing the number of iterations
- It produces more varied play than a fixed heuristic

MCTS works by incrementally building a search tree. Each iteration consists of four steps: 
1. **Selecting** a path through the tree to a leaf node, 
2. **Expanding** the tree by adding a new node for an unexplored move,
3. **Simulating** the game from that node using a fast policy, and finally
4. **Backpropagating** the result up the path taken.

Node selection uses the Upper Confidence Bound for Trees (UCT) formula:

$$UCT(i) = \frac{w_i}{n_i} + C \cdot \sqrt{\frac{\ln N_i}{n_i}}$$

where

- $w_i$: number of wins accumulated at node $i$
- $n_i$: number of times node $i$ has been visited
- $N_i$: number of times its parent has been visited
- $C$: exploration constant

The first term is the **exploitation** term, favoring nodes that have yielded good results so far. 
The second term is the **exploration** term, favoring nodes that have been visited less often relative to their parent.

The balance between these two terms is controlled by the exploration constant, $C$.

### Considerations for Barbu

#### Scoring
Barbu is not a binary win-or-lose game. The result is determined by cumulative scores across all contracts, 
so the win count $w_i$ does not apply directly. 
Instead, each simulation is evaluated by its margin, defined as: 
- If the highest score wins: bot's score minus the highest opponent's score
- If the lowest score wins: lowest opponent's score minus the bot's score.

A positive margin means the bot is currently ahead. 
This margin is the value backpropagated up the tree, so $w_i$ accumulates margins rather than wins,
and $w_i / n_i$ represents the node's average margin. To keep the exploration constant $C$ balanced across contracts
with very different scoring ranges, this average margin is normalized to $[0,1]$ against the lowest and highest
margins observed during the search. This normalized value serves as the exploitation term in the UCT formula.

#### Simulation depth
Simulations terminate at the end of the current contract rather than at the end of the game. 
The hands for each contract are dealt independently, 
so the expected margin contributions from future contracts are identical across all candidate moves, and
they therefore cancel out when comparing options. 
This holds for both in-contract moves and contract selection.

#### Imperfect information 
MCTS assumes a fully-known game state, but in Barbu the opponents' hands are hidden. 
A determinization replaces unknown opponent hands with a plausible sample, producing a fully-known state the algorithm can search. 
The `Determinizer` distributes the remaining cards among opponents using the information available to the bot: 
its own hand, the cards already played, and the `impossibleCards` constraints tracked by the engine.

The main difference between the two MCTS implementations is their handling of this determinization:

* `MctsBot` builds one independent search tree per determinization and runs them in parallel. 
Each tree votes for its best move; the move with the most votes wins. 
* `IsmctsBot` instead maintains a single shared tree across all determinizations, 
sampling a new one at the start of each iteration. Because some nodes are only reachable under certain determinizations, 
their visit count underrepresents how often they could have been selected. 
To account for this, each node tracks `timesAvailable` (the number of iterations in which it was reachable), 
and the UCT formula uses this count instead of the parent visit count.

### Testing

To evaluate bot performance and tune configuration parameters, 
a `mcts-simulations` module (not included in this submission) was developed to run bot-vs-bot games and write the results to CSV files for analysis. 
The benchmarks compared MCTS and IS-MCTS bots across different iteration budgets and measured performance against both `RandomMoveBot` and
`HeuristicBot` opponents. 

The `HeuristicBot` (also not included in this submission) is a rule-based opponent: rather than searching, it selects moves based on fixed rules.
It is considerably stronger than random, winning around 80% of games against 3 `RandomMoveBot` opponents in the Standard Barbu variant. 
It was developed in order to assess how well the MCTS bots perform against stronger opponents.

These results served as an empirical baseline for the choice of iteration counts and the exploration constant for the Medium and Hard difficulty bots used in the application.

---

## API

The API layer has already been documented by my colleagues.
This section covers selected design decisions not addressed there.

### State Transitions and the Frontend

When the last card of a trick is played, the engine advances the state internally: 
the trick is processed and the state moves forward to either an empty trick or the next phase.
The completed trick is not part of the resulting state, but the frontend needs to display it. 
The same applies to the final tableau at the end of a Réussite contract.

The solution is implemented at the API level: after each move, the server checks whether a trick or tableau was just completed,
reconstructs it from the previous state plus the move, and includes it in the `PublicGameUpdateDTO`.

### Data Model

No mutable game state is stored in the database. 
A game is persisted as its settings, seed, and an append-only log of moves as `GameMoveEntity` rows. 
When the current state is needed, `GameState.replay(settings, seed, moves)` reconstructs it from these.

`PlayerEntity` uses JPA joined inheritance: common fields live in a shared `game_players` table. 
`HumanPlayerEntity` adds a row in `human_players` with a foreign key to the user account. 
`BotPlayerEntity` adds a row in `bot_players` with the bot type and display name.

### Barbu Catalog

The `GameCatalog` defines the Barbu variants used by the application, each as a `GameVariant`.
A `GameVariant` is the catalog-layer counterpart of the engine's `GameSettings`, adding a stable ID and a human-readable name.
Its contracts are wrapped in the same way: each `ContractDefinition` pairs an engine `Contract` with its own ID and display name.

Adding a new variant to the application requires only a new `GameVariant` definition in `GameCatalog`; 
no further API or frontend changes are needed.

Initially, `GameEntity` stored the variant ID as a reference to the catalog. Because this was a de facto foreign key
without database enforcement, renaming or removing a variant in the catalog could silently break ongoing games.
To fix this, `GameEntity` now stores a complete snapshot of the variant information directly. 
At creation time, it generates a `GameContractEntity` row for every contract, linked via a proper foreign key to the `GameEntity`.
As a result, a single game is now self-contained and no longer depends on the catalog remaining unchanged.

`GameCatalog` was later extracted into its own `barbu-catalog` module so that it could be reused by the `mcts-simulations` module 
without introducing an unnecessary dependency on the API and Spring.