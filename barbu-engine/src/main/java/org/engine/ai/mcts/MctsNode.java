package org.engine.ai.mcts;

import org.engine.ai.BaseSearchNode;
import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.GameState;
import org.engine.game.state.WaitingForContractSelection;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;

/**
 * MCTS tree node.
 * <p>
 * Stores the full game state and a shuffled list of unexplored moves
 * so that expansion order is effectively random.
 */
public class MctsNode extends BaseSearchNode<MctsNode> {
    /** Game state at this node. */
    private final GameState gameState;
    /** Legal moves not yet expanded from this node, shuffled to randomize expansion order. */
    private ArrayList<Move> unexploredMoves;

    public MctsNode(GameState gameState, @Nullable MctsNode parent, @Nullable Move moveFromParent) {
        super(parent, moveFromParent);
        this.gameState = gameState;

        boolean isPhaseBoundary = (this.parent != null && this.gameState instanceof WaitingForContractSelection);

        if (!isPhaseBoundary && this.gameState instanceof ActiveGameState activeState) {
            this.unexploredMoves = new ArrayList<>(activeState.legalMoves());
            Collections.shuffle(this.unexploredMoves);
        } else {
            this.unexploredMoves = new ArrayList<>();
        }
    }
    
    GameState getGameState() {
        return gameState;
    }

    public static MctsNode createRoot(ActiveGameState initialState) {
        return new MctsNode(initialState, null, null);
    }

    /** True when the node has no unexplored moves and no children.
     * This indicates the end of the search depth, not necessarily a game-over state.
     * The search ends at the end of a contract.
     */
    public boolean isTerminal() {
        return this.unexploredMoves.isEmpty() && this.children.isEmpty();
    }

    /** Returns the player to move at this node's state. */
    public Player getActivePlayer() {
        if (this.gameState instanceof ActiveGameState active) {
            return active.currentPlayer();
        }
        throw new IllegalStateException("Cannot get active player: Game state is not active (Game Over).");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculateUCT(double explorationConst,
                               double[] globalMinMargins,
                               double[] globalMaxMargins,
                               int parentActivePlayerIndex) {
        if (this.parent == null) {
            throw new IllegalStateException("cannot calculate UCT of root node");
        }
        if (this.timesExplored == 0) {
            throw new IllegalStateException("UCT called on unexplored node");
        }

        double globalMin = globalMinMargins[parentActivePlayerIndex];
        double globalMax = globalMaxMargins[parentActivePlayerIndex];

        double exploitation = this.normalizeScore(parentActivePlayerIndex, globalMin, globalMax);
        double exploration = explorationConst * Math.sqrt(Math.log(this.parent.timesExplored) / this.timesExplored);

        return exploitation + exploration;
    }

    /** * Descends the tree using UCT until it reaches a node with unexplored moves, or the end of the contract.
     *
     * @param explorationConst weight for the exploration term
     * @param globalMinMargins per-player running minimum of all simulation margins
     * @param globalMaxMargins per-player running maximum of all simulation margins
     * @return the selected leaf node
     */
    MctsNode select(double explorationConst, double[] globalMinMargins, double[] globalMaxMargins) {
        MctsNode current = this;
        while (current.unexploredMoves.isEmpty() && !current.children.isEmpty()) {
            current = current.selectBestUCTChild(explorationConst, globalMinMargins, globalMaxMargins);
        }
        return current;
    }

    private MctsNode selectBestUCTChild(double explorationConst, double[] globalMinMargins, double[] globalMaxMargins) {
        MctsNode bestChild = null;
        double bestUCT = Double.NEGATIVE_INFINITY;
        int activePlayerIndex = this.getActivePlayer().ordinal();

        for (MctsNode child : this.children) {
            double uctValue = child.calculateUCT(explorationConst, globalMinMargins, globalMaxMargins, activePlayerIndex);
            if (uctValue > bestUCT) {
                bestUCT = uctValue;
                bestChild = child;
            }
        }

        if (bestChild == null) {
            throw new IllegalStateException("getBestUCTChild called on node with no children");
        }
        return bestChild;
    }

    /** Pops the next unexplored move, applies it to create a child node, and returns that child. */
    MctsNode expand() {
        if (!(this.gameState instanceof ActiveGameState active)) {
            throw new IllegalStateException("Cannot expand a final game state");
        }
        Move move = this.unexploredMoves.removeLast();
        MctsNode expanded = new MctsNode(active.applyMove(move), this, move);
        this.children.add(expanded);
        return expanded;
    }
}