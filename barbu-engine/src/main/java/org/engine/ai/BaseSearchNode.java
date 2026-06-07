package org.engine.ai;

import org.engine.game.GameSettings;
import org.engine.game.Move;
import org.engine.game.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract node in a game-tree search.
 * <p>
 * Maintains visit counts, accumulated margin sums, and parent/child links
 * shared by all implemented search algorithms.
 */
public abstract class BaseSearchNode<T extends BaseSearchNode<T>> {
    protected @Nullable T parent;
    protected @Nullable Move moveFromParent;
    protected List<T> children;
    /** Number of times this node has been visited across all iterations. */
    protected int timesExplored;
    /** Per-player sum of simulation margins accumulated during backpropagation. */
    protected double[] marginSums;

    protected BaseSearchNode(@Nullable T parent, @Nullable Move moveFromParent) {
        this.parent = parent;
        this.moveFromParent = moveFromParent;
        this.children = new ArrayList<>();
        this.timesExplored = 0;
        this.marginSums = new double[Player.values().length];
    }

    public @Nullable Move getMoveFromParent() {
        return moveFromParent;
    }

    public List<T> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Calculates the Upper Confidence Bound applied to Trees (UCT) value.
     *
     * <p>UCT = normalised_exploitation + C * sqrt(ln(timesAvailable) / timesExplored)
     *
     * <p>Exploitation is the average margin for the active player, normalised to [0, 1]
     * using the global min/max bounds tracked by the tree.
     *
     * @param explorationConst weight for the exploration term
     * @param globalMinMargins per-player running minimum of all simulation margins
     * @param globalMaxMargins per-player running maximum of all simulation margins
     * @param parentActivePlayerIndex ordinal of the player to move at the parent node
     * @return the computed UCT score
     */
    public abstract double calculateUCT(double explorationConst,
                                        double[] globalMinMargins,
                                        double[] globalMaxMargins,
                                        int parentActivePlayerIndex);

    /**
     * Normalizes the average margin for a specific player into a [0, 1] range.
     *
     * @param playerIndex ordinal of the player
     * @param globalMin lowest margin observed for this player
     * @param globalMax highest margin observed for this player
     * @return the normalized score
     */
    protected double normalizeScore(int playerIndex, double globalMin, double globalMax) {
        if (this.timesExplored == 0) {
            throw new IllegalStateException("normalizeScore called on unexplored node");
        }
        double averageMargin = this.marginSums[playerIndex] / this.timesExplored;
        if (globalMax == globalMin) {
            // avoid division by zero
            return 0.5;
        }
        return (averageMargin - globalMin) / (globalMax - globalMin);
    }

    /**
     * Ascends the tree from this node to the root, incrementing visit counts and
     * accumulating margins at each ancestor.
     *
     * @param margins the array of per-player margins to add to the running sums
     */
    public void backpropagate(double[] margins) {
        BaseSearchNode<T> current = this;
        while (current != null) {
            current.timesExplored++;
            for (int i = 0; i < margins.length; i++) {
                current.marginSums[i] += margins[i];
            }
            current = current.parent;
        }
    }

    /** Returns the move leading to the most-visited child */
    public Move getBestMove() {
        if (children.isEmpty()) {
            throw new IllegalStateException("No moves explored");
        }
        T best = children.getFirst();
        for (T child : children) {
            if (child.timesExplored > best.timesExplored) {
                best = child;
            }
        }
        assert best.moveFromParent != null;
        return best.moveFromParent;
    }

    /**
     * Computes each player's score advantage relative to the best-scoring opponent.
     * If the game settings dictate that the lowest score wins, this margin is negated.
     *
     * @param finalScores the final scores mapped by player
     * @param rankingOrder the ruleset determining whether high or low scores win
     * @return an array of calculated margins ordered by player ordinal
     */
    public static double[] calculateMargins(Map<Player, Integer> finalScores, GameSettings.RankingOrder rankingOrder) {
        Player[] players = Player.values();
        double[] margins = new double[players.length];
        boolean lowestWins = rankingOrder == GameSettings.RankingOrder.LOWEST_SCORE_WINS;
        for (Player currentPlayer : players) {
            double bestOpponentScore = lowestWins ? Double.MAX_VALUE : Double.NEGATIVE_INFINITY;
            for (Player opponent : players) {
                if (currentPlayer != opponent) {
                    bestOpponentScore = lowestWins
                            ? Math.min(bestOpponentScore, finalScores.get(opponent))
                            : Math.max(bestOpponentScore, finalScores.get(opponent));
                }
            }
            margins[currentPlayer.ordinal()] = lowestWins
                    ? bestOpponentScore - finalScores.get(currentPlayer)
                    : finalScores.get(currentPlayer) - bestOpponentScore;
        }
        return margins;
    }
}