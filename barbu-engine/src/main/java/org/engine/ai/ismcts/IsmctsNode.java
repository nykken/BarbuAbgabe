package org.engine.ai.ismcts;

import org.engine.ai.BaseSearchNode;
import org.engine.game.Move;
import org.jspecify.annotations.Nullable;

/**
 * Search tree node for Information Set Monte Carlo Tree Search
 */
public class IsmctsNode extends BaseSearchNode<IsmctsNode> {

    /**
     * Number of iterations in which this node was present in the compatible set at its parent.
     * Used as the denominator of the UCT exploration term in place of the parent's visit count,
     * so that nodes unavailable under some determinizations are not penalised for low visit counts.
     */
    int timesAvailable;

    /**
     * Creates a node with the given parent and the move that leads to it.
     *
     * @param parent         the parent node, or {@code null} for the root
     * @param moveFromParent the move from the parent that reaches this node,
     *                       or {@code null} for the root
     */
    public IsmctsNode(@Nullable IsmctsNode parent, @Nullable Move moveFromParent) {
        super(parent, moveFromParent);
        this.timesAvailable = 0;
    }

    /**
     * Creates a child node for {@code move} and attaches it to this node.
     *
     * @param move the move the child represents
     * @return the newly created child node
     */
    public IsmctsNode expand(Move move) {
        IsmctsNode child = new IsmctsNode(this, move);
        this.children.add(child);
        return child;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public double calculateUCT(double explorationConst,
                               double[] globalMinMargins,
                               double[] globalMaxMargins,
                               int parentActivePlayerIndex) {
        if (this.timesExplored == 0) {
            throw new IllegalStateException("calculateUCT called on unexplored node");
        }

        double globalMin = globalMinMargins[parentActivePlayerIndex];
        double globalMax = globalMaxMargins[parentActivePlayerIndex];

        double exploitation = this.normalizeScore(parentActivePlayerIndex, globalMin, globalMax);
        double exploration = explorationConst * Math.sqrt(Math.log(this.timesAvailable) / this.timesExplored);

        return exploitation + exploration;
    }
}