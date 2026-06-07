package org.engine.ai.mcts;

import org.engine.ai.Bot;
import org.engine.ai.Determinizer;
import org.engine.ai.RandomMoveBot;
import org.engine.card.Hand;
import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.WaitingForContractSelection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Monte Carlo Tree Search bot using determinized parallel tree voting.
 * <p>
 * Runs {@code numTrees} independent {@link MctsTree} instances in parallel, each on a different
 * determinization of the hidden card distribution. Each tree votes for its best move;
 * the move with the most votes is returned.
 * <p>
 * Constructors that omit a {@code simulationBot} default to {@link org.engine.ai.RandomMoveBot}.
 */
public class MctsBot extends Bot {
    private static final ExecutorService MCTS_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final double explorationConstant;
    private final Bot simulationBot;
    private final int contractNumTrees;
    private final int contractIterationsPerTree;
    private final int playNumTrees;
    private final int playIterationsPerTree;

    public MctsBot(double explorationConstant, int numTrees, int iterationsPerTree) {
        this(new RandomMoveBot(), explorationConstant, numTrees, iterationsPerTree);
    }

    public MctsBot(double explorationConstant,
                   int contractNumTrees, int contractIterationsPerTree,
                   int playNumTrees, int playIterationsPerTree) {
        this(new RandomMoveBot(), explorationConstant,
                contractNumTrees, contractIterationsPerTree,
                playNumTrees, playIterationsPerTree);
    }

    public MctsBot(Bot simulationBot, double explorationConstant,
                   int numTrees, int iterationsPerTree) {
        this(simulationBot, explorationConstant,
                numTrees, iterationsPerTree, numTrees, iterationsPerTree);
    }

    /**
     * Primary constructor for the MCTS bot.
     *
     * @param simulationBot bot used for simulations
     * @param explorationConstant weight for the UCT exploration term
     * @param contractNumTrees number of parallel trees during contract selection
     * @param contractIterationsPerTree iterations per tree during contract selection
     * @param playNumTrees number of parallel trees during the card-play phase
     * @param playIterationsPerTree iterations per tree during the card-play phase
     */
    public MctsBot(Bot simulationBot, double explorationConstant,
                   int contractNumTrees, int contractIterationsPerTree,
                   int playNumTrees, int playIterationsPerTree) {
        if (contractNumTrees <= 0) throw new IllegalArgumentException("contractNumTrees must be > 0");
        if (contractIterationsPerTree <= 0) throw new IllegalArgumentException("contractIterationsPerTree must be > 0");
        if (playNumTrees <= 0) throw new IllegalArgumentException("playNumTrees must be > 0");
        if (playIterationsPerTree <= 0) throw new IllegalArgumentException("playIterationsPerTree must be > 0");
        this.simulationBot = simulationBot;
        this.explorationConstant = explorationConstant;
        this.contractNumTrees = contractNumTrees;
        this.contractIterationsPerTree = contractIterationsPerTree;
        this.playNumTrees = playNumTrees;
        this.playIterationsPerTree = playIterationsPerTree;
    }

    /**
     * Executes the parallel MCTS algorithm on the provided state.
     *
     * @param state the current active game state
     * @return the most-voted move
     */
    @Override
    protected Move search(ActiveGameState state) {
        Player currentPlayer = state.currentPlayer();

        boolean isContractSelection = state instanceof WaitingForContractSelection;
        int trees = isContractSelection ? contractNumTrees : playNumTrees;
        int iterations = isContractSelection ? contractIterationsPerTree : playIterationsPerTree;

        List<CompletableFuture<Move>> futures = IntStream.range(0, trees)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    ThreadLocalRandom localRng = ThreadLocalRandom.current();
                    Map<Player, Hand> determinizedHands = Determinizer.sampleHands(state, currentPlayer, localRng);
                    ActiveGameState determinizedState = state.withHands(determinizedHands);

                    MctsTree tree = new MctsTree(determinizedState, explorationConstant, simulationBot);
                    for (int j = 0; j < iterations; j++) tree.runIteration();
                    return tree.getBestMove();
                }, MCTS_EXECUTOR))
                .toList();

        List<Move> bestMoves = futures.stream().map(CompletableFuture::join).toList();

        // each tree votes for the best move
        Map<Move, Integer> moveVotes = new HashMap<>();
        for (Move move : bestMoves) {
            moveVotes.merge(move, 1, Integer::sum);
        }

        return getMostVotedMove(moveVotes);
    }


    @Override
    public String describe() {
        String base = (contractNumTrees == playNumTrees && contractIterationsPerTree == playIterationsPerTree)
                ? String.format("MCTS(trees=%d,iter=%d,c=%.1f)", contractNumTrees, contractIterationsPerTree, explorationConstant)
                : String.format("MCTS(cTrees=%d,cIter=%d,pTrees=%d,pIter=%d,c=%.1f)",
                        contractNumTrees, contractIterationsPerTree, playNumTrees, playIterationsPerTree, explorationConstant);
        return simulationBot instanceof RandomMoveBot ? base : base + ",sim=" + simulationBot.describe();
    }

    private Move getMostVotedMove(Map<Move, Integer> moveVotes) {
        return moveVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new IllegalStateException("no votes cast: search ran with zero trees"))
                .getKey();
    }
}