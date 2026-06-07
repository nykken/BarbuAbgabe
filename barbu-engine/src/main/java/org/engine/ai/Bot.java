package org.engine.ai;

import org.engine.game.Move;
import org.engine.game.state.ActiveGameState;

import java.util.List;

/**
 * Base class for bot players.
 *
 * <p>Implementations decide which {@link Move} to make given the current game state.
 */
public abstract class Bot {
    /**
     * Core search logic. Called only when there are at least two legal moves.
     */
    protected abstract Move search(ActiveGameState state);

    /**
     * Entry point for all callers. Short-circuits when only one move is legal,
     * avoiding unnecessary search work in all implementations.
     */
    public Move chooseMove(ActiveGameState state) {
        List<Move> legal = state.legalMoves();
        if (legal.size() == 1) return legal.getFirst();
        return search(state);
    }

    public String describe() { return getClass().getSimpleName(); }
}