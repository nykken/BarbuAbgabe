package org.engine.ai;

import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ContractInProgress;
import org.engine.game.state.GameState;

/**
 * Base class for MCTS-style search trees.
 * <p>
 * Owns the root node, simulation bot, exploration constant, and per-player
 * global margin bounds used for UCT score normalisation.
 */
public abstract class BaseSearchTree<N extends BaseSearchNode<N>> {
    protected final N root;
    protected final Bot simulationBot;
    protected final double explorationConstant;
    protected final double[] globalMinMargins;
    protected final double[] globalMaxMargins;

    protected BaseSearchTree(N root, double explorationConstant, Bot simulationBot) {
        this.root = root;
        this.explorationConstant = explorationConstant;
        this.simulationBot = simulationBot;
        int numPlayers = Player.values().length;
        this.globalMinMargins = new double[numPlayers];
        this.globalMaxMargins = new double[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            this.globalMinMargins[i] = Double.MAX_VALUE;
            this.globalMaxMargins[i] = Double.NEGATIVE_INFINITY;
        }
    }

    /** Updates the global margin bounds with the latest simulation results. */
    protected void updateGlobalBounds(double[] margins) {
        for (int i = 0; i < margins.length; i++) {
            this.globalMinMargins[i] = Math.min(this.globalMinMargins[i], margins[i]);
            this.globalMaxMargins[i] = Math.max(this.globalMaxMargins[i], margins[i]);
        }
    }

    /**
     * Plays out from {@code leafState} to the end of the contract using the simulation bot.
     * Returns the resulting margins.
     */
    protected double[] simulate(GameState leafState) {
        GameState currentState = leafState;
        while (currentState instanceof ContractInProgress contractState) {
            Move move = simulationBot.chooseMove(contractState);
            currentState = contractState.applyMove(move);
        }
        return BaseSearchNode.calculateMargins(currentState.cumulativeScores(), currentState.settings().rankingOrder());
    }

    /** Delegates to the root node's best-move selection. */
    public Move getBestMove() {
        return root.getBestMove();
    }
}