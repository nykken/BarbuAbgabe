package org.engine.ai.ismcts;

import org.engine.ai.BaseSearchTree;
import org.engine.ai.Bot;
import org.engine.game.Move;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.GameState;
import org.engine.game.state.WaitingForContractSelection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Search tree for Information Set Monte Carlo Tree Search.
 *
 * <p>A single tree instance is built up across all iterations of one {@link IsmctsBot}
 * search. Each call to {@link #runIteration} operates on a new
 * determinization of the hidden information, so the tree aggregates statistics over
 * many possible worlds.
 */
public class IsmctsTree extends BaseSearchTree<IsmctsNode> {


    public IsmctsTree(double explorationConstant, Bot simulationBot) {
        super(new IsmctsNode(null, null), explorationConstant, simulationBot);
    }

    /**
     * Runs one complete ISMCTS iteration on the given determinized state.
     *
     * <p>The iteration has three phases:
     * <ol>
     *   <li><b>Selection / expansion:</b> descends the tree using UCT until either an
     *       unexpanded move is found (which is immediately expanded) or a terminal /
     *       contract-selection boundary is reached. At each interior node only children
     *       whose move is legal in the current determinization are considered
     *       ({@code compatible} set); their {@code timesAvailable} counters are
     *       incremented.</li>
     *   <li><b>Simulation:</b> plays the game to completion from the reached node
     *       using the simulation bot, producing an array with the margins for each player</li>
     *   <li><b>Backpropagation:</b> propagates the margins up to the root, updating
     *       {@code timesExplored} and {@code marginSums} on every ancestor.</li>
     * </ol>
     *
     * @param determinizedState a determinized ActiveGameState for this iteration
     * @throws IllegalStateException if the tree is in an inconsistent state
     */
    public void runIteration(ActiveGameState determinizedState) {
        IsmctsNode currentNode = root;
        GameState currentState = determinizedState;


        // selection and expansion
        while ((currentState instanceof ActiveGameState activeGameState)
        && (!(currentNode != root && activeGameState instanceof WaitingForContractSelection))) {

            Set<Move> legalSet = new HashSet<>(activeGameState.legalMoves());

            List<IsmctsNode> compatibleNodes = new ArrayList<>();
            Set<Move> expandedMoves = new HashSet<>();
            for (IsmctsNode child : currentNode.getChildren()) {
                if (legalSet.contains(child.getMoveFromParent())) {
                    compatibleNodes.add(child);
                    assert child.getMoveFromParent() != null;
                    expandedMoves.add(child.getMoveFromParent());
                }
            }

            for (IsmctsNode child : compatibleNodes) {
                child.timesAvailable++;
            }


            List<Move> unexpanded = new ArrayList<>();
            for (Move move : legalSet) {
                if (!expandedMoves.contains(move)) {
                    unexpanded.add(move);
                }
            }

            if (!unexpanded.isEmpty()) {
                Move move = unexpanded.get(ThreadLocalRandom.current().nextInt(unexpanded.size()));
                IsmctsNode child = currentNode.expand(move);
                child.timesAvailable++;
                currentState = activeGameState.applyMove(move);
                currentNode = child;
                break;
            }

            if (compatibleNodes.isEmpty()) {
                throw new IllegalStateException("no compatible nodes but state is active");
            }

            int activePlayerIndex = activeGameState.currentPlayer().ordinal();
            IsmctsNode bestChild = selectBestUCTChild(compatibleNodes, activePlayerIndex);
            if (bestChild.getMoveFromParent() == null) {
                throw new IllegalStateException("best child's move from parent is null");
            }

            currentState = activeGameState.applyMove(bestChild.getMoveFromParent());
            currentNode = bestChild;
        }

        double[] margins = this.simulate(currentState);
        updateGlobalBounds(margins);
        currentNode.backpropagate(margins);
    }

    /**
     * Selects the child with the highest UCT score from the compatible set.
     *
     * @param compatible        nodes whose move is legal in the current determinization
     * @param activePlayerIndex ordinal of the player to move at the parent node
     * @return the child with the highest UCT value
     * @throws IllegalStateException if {@code compatible} is empty
     */
    private IsmctsNode selectBestUCTChild(List<IsmctsNode> compatible, int activePlayerIndex) {
        IsmctsNode best = null;
        double bestUCT = Double.NEGATIVE_INFINITY;
        for (IsmctsNode child : compatible) {
            double uct = child.calculateUCT(explorationConstant, globalMinMargins, globalMaxMargins, activePlayerIndex);
            if (uct > bestUCT) {
                bestUCT = uct;
                best = child;
            }
        }
        if (best == null) {
            throw new IllegalStateException("selectBestUCT called with empty list");
        }
        return best;
    }
}