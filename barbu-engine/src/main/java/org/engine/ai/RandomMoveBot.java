package org.engine.ai;

import org.engine.game.Move;
import org.engine.game.state.ActiveGameState;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bot that selects a legal move at random.
 */
public class RandomMoveBot extends Bot {
    /**
     * Picks a random move from the set of legal moves.
     *
     * @param state the current active game state
     * @return a randomly chosen legal move
     */
    @Override
    protected Move search(ActiveGameState state) {
        List<Move> legal = state.legalMoves();
        return legal.get(ThreadLocalRandom.current().nextInt(legal.size()));
    }
}