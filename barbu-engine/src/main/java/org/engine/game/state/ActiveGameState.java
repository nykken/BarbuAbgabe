package org.engine.game.state;

import org.engine.card.Hand;
import org.engine.game.GameSettings;
import org.engine.game.History;
import org.engine.game.Move;
import org.engine.game.Player;

import java.util.List;
import java.util.Map;

/**
 * Base for all non-terminal game states. Only these states accept moves, have a current player and legal moves,
 * so the compiler enforces you cannot call those methods on a {@link GameOver}.
 */
public sealed abstract class ActiveGameState extends GameState permits ContractInProgress, WaitingForContractSelection {

    protected ActiveGameState(GameSettings settings, History history) {
        super(settings, history);
    }

    /** Returns the player whose turn it is to act. */
    public abstract Player currentPlayer();

    /** Returns the player currently responsible for declaring contracts. */
    public abstract Player currentDeclarer();

    /** Returns each player's current hand. */
    public abstract Map<Player, Hand> hands();

    /** Returns all moves the current player may legally make. */
    public abstract List<Move> legalMoves();

    /**
     * Applies a move to this state and returns the resulting state.
     * Each subclass accepts only the moves legal in its phase.
     *
     * @throws IllegalStateException if the move type is not legal in the current phase
     * @throws org.engine.contract.IllegalMoveException if the move violates game rules
     */
    public abstract GameState applyMove(Move move);

    /**
     * Creates a copy of this state, replacing the current hands with the provided ones.
     * Used by AI for determinization.
     *
     * @param newHands the hypothetical distribution of cards
     * @return a new state identical to this one, but with the new hands
     */
    public abstract ActiveGameState withHands(Map<Player, Hand> newHands);
}