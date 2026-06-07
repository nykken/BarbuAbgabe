package org.engine.ai.mcts;

import org.engine.ai.BaseSearchTree;
import org.engine.ai.Bot;
import org.engine.game.state.ActiveGameState;

/** Thin orchestrator that runs one complete MCTS iteration on a determinized game state. */
public class MctsTree extends BaseSearchTree<MctsNode> {

    public MctsTree(ActiveGameState initialState, double explorationConstant, Bot simulationBot) {
        super(MctsNode.createRoot(initialState), explorationConstant, simulationBot);
    }
    
    public void runIteration() {
        MctsNode leaf = root.select(this.explorationConstant, this.globalMinMargins, this.globalMaxMargins);

        if (!leaf.isTerminal()) {
            leaf = leaf.expand();
        }

        double[] simulatedMargins = this.simulate(leaf.getGameState());

        updateGlobalBounds(simulatedMargins);

        leaf.backpropagate(simulatedMargins);
    }
}