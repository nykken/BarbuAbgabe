package org.engine.ai.ismcts;

import org.engine.ai.Bot;
import org.engine.ai.Determinizer;
import org.engine.ai.RandomMoveBot;
import org.engine.card.Hand;
import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.WaitingForContractSelection;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bot that uses Information Set Monte Carlo Tree Search (ISMCTS) to select a Barbu move.
 * Separate iteration budgets can be configured for the contract selection and card play.
 * <p>
 * Constructors that omit a {@code simulationBot} default to {@link org.engine.ai.RandomMoveBot}.
 */
public class IsmctsBot extends Bot {
    private final Bot simulationBot;
    private final double explorationConstant;
    private final int contractTotalIterations;
    private final int playTotalIterations;


    public IsmctsBot(double explorationConstant, int totalIterations) {
        this(new RandomMoveBot(), explorationConstant, totalIterations, totalIterations);
    }

    public IsmctsBot(double explorationConstant, int contractTotalIterations, int playTotalIterations) {
        this(new RandomMoveBot(), explorationConstant, contractTotalIterations, playTotalIterations);
    }

    public IsmctsBot(Bot simulationBot, double explorationConstant, int totalIterations) {
        this(simulationBot, explorationConstant, totalIterations, totalIterations);
    }

    /**
     * Primary constructor for the ISMCTS bot.
     *
     * @param simulationBot bot used for simulations
     * @param explorationConstant weight for the UCT exploration term
     * @param contractTotalIterations iterations for the contract-selection phase
     * @param playTotalIterations iterations for the card-play phase
     */
    public IsmctsBot(Bot simulationBot, double explorationConstant,
                     int contractTotalIterations, int playTotalIterations) {
        if (contractTotalIterations <= 0) throw new IllegalArgumentException("contractTotalIterations must be > 0");
        if (playTotalIterations <= 0) throw new IllegalArgumentException("playTotalIterations must be > 0");
        this.simulationBot = simulationBot;
        this.explorationConstant = explorationConstant;
        this.contractTotalIterations = contractTotalIterations;
        this.playTotalIterations = playTotalIterations;
    }

    /**
     * Executes the ISMCTS algorithm on the provided state.
     *
     * @param state the current active game state
     * @return the selected move
     */
    @Override
    protected Move search(ActiveGameState state) {
        Player pov = state.currentPlayer();
        IsmctsTree tree = new IsmctsTree(explorationConstant, simulationBot);

        int iterations = state instanceof WaitingForContractSelection
                ? contractTotalIterations : playTotalIterations;
        for (int i = 0; i < iterations; i++) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            Map<Player, Hand> hands = Determinizer.sampleHands(state, pov, rng);
            ActiveGameState deterministicState = state.withHands(hands);
            tree.runIteration(deterministicState);
        }

        return tree.getBestMove();
    }

    @Override
    public String describe() {
        String base = (contractTotalIterations == playTotalIterations)
                ? String.format("ISMCTS(iter=%d,c=%.1f)", contractTotalIterations, explorationConstant)
                : String.format("ISMCTS(cIter=%d,pIter=%d,c=%.1f)", contractTotalIterations, playTotalIterations, explorationConstant);
        return simulationBot instanceof RandomMoveBot ? base : base + ",sim=" + simulationBot.describe();
    }
}