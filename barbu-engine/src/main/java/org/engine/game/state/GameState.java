package org.engine.game.state;

import org.engine.contract.Contract;
import org.engine.game.*;

import java.util.*;

/**
 * The complete, immutable state of a Barbu game at a point in time.
 *
 * <p>Two direct permitted subclasses split the hierarchy:
 * <ul>
 *   <li>{@link ActiveGameState}: game is still in progress. Accepts moves via
 *       {@link ActiveGameState#applyMove}. Further subclassed into
 *       {@link WaitingForContractSelection} and
 *       {@link ContractInProgress} </li>
 *   <li>{@link GameOver}: all declarers have completed all their contracts, no
 *       further moves are accepted.</li>
 * </ul>
 *
 * Never mutated in place.
 */
public abstract sealed class GameState
        permits ActiveGameState, GameOver {

    private final GameSettings settings;
    private final History history;

    protected GameState(GameSettings settings, History history) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.history = Objects.requireNonNull(history, "history must not be null");
    }
    
    public GameSettings settings() { return settings; }

    /** Returns the completed-contract history accumulated so far. */
    public History history()       { return history; }

    /**
     * Reconstructs the current state by replaying {@code moves} from scratch.
     * Equivalent to calling {@link #newGame} then applying each move in order.
     */
    public static GameState replay(GameSettings settings, long seed, List<Move> moves) {
        GameState state = newGame(settings, seed);
        for (Move move : moves) state = ((ActiveGameState) state).applyMove(move);
        return state;
    }

    /** Creates the initial state for a new game and deals the first hands. */
    public static GameState newGame(GameSettings settings, long initialSeed) {
        return new WaitingForContractSelection(settings, History.create(), Player.STARTING_PLAYER, initialSeed);
    }

    /** Returns per-player scores summed across all completed contracts. */
    public Map<Player, Integer> cumulativeScores() {
        return history.cumulativeScores();
    }


    /** Returns the contracts {@code player} has yet to declare. */
    public List<Contract> remainingContracts(Player player) {
        List<Contract> remaining = new ArrayList<>(settings.contracts());

        List<Contract> played = history.finishedContractsForDeclarer(player).stream()
                .map(History.FinishedContract::contract)
                .toList();

        for (Contract contract : played) {
            remaining.remove(contract);
        }

        return List.copyOf(remaining);
    }
}