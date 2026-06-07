package org.engine.game.state;

import org.engine.card.Hand;
import org.engine.contract.ContractState;
import org.engine.game.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Game state while players are playing cards in an active contract.
 */
public final class ContractInProgress extends ActiveGameState {
    private final Player currentDeclarer;
    private final ContractState activeContract;
    private final long rngSeed;

    ContractInProgress(GameSettings settings, History history,
                       Player currentDeclarer, ContractState activeContract, long rngSeed) {
        super(settings, history);
        this.currentDeclarer = Objects.requireNonNull(currentDeclarer);
        this.activeContract  = Objects.requireNonNull(activeContract);
        this.rngSeed = rngSeed;
    }

    @Override
    public Player currentDeclarer() { return currentDeclarer; }

    /** Returns the state of the contract currently being played. */
    public ContractState activeContract() { return activeContract; }

    @Override
    public Player currentPlayer() { return activeContract.currentPlayer(); }

    @Override
    public Map<Player, Hand> hands() { return activeContract.hands(); }

    @Override
    public List<Move> legalMoves() { return activeContract.legalMoves(); }

    @Override
    public GameState applyMove(Move move) {
        ContractState updated = activeContract.applyMove(move);
        if (updated.isFinished()) {
            return finishContract(updated);
        }
        return new ContractInProgress(settings(), history(), currentDeclarer, updated, rngSeed);
    }

    private GameState finishContract(ContractState finished) {
        History updatedHistory = history().with(currentDeclarer, finished);

        if (hasDeclarerFinishedAllContracts(updatedHistory)) {
            return nextDeclarerOrGameOver(updatedHistory);
        }

        return new WaitingForContractSelection(settings(), updatedHistory, currentDeclarer, rngSeed);
    }

    private boolean hasDeclarerFinishedAllContracts(History history) {
        int finishedCount = history.finishedContractsForDeclarer(currentDeclarer).size();
        return finishedCount == settings().contracts().size();
    }

    private GameState nextDeclarerOrGameOver(History history) {
        Player nextDeclarer = currentDeclarer.next();

        if (nextDeclarer == Player.STARTING_PLAYER) {
            return new GameOver(settings(), history);
        }

        return new WaitingForContractSelection(settings(), history, nextDeclarer, rngSeed);
    }

    @Override
    public ActiveGameState withHands(Map<Player, Hand> newHands) {
        // Delegate hand-swapping to the active contract
        ContractState contractWithHands = activeContract.withHands(newHands);

        return new ContractInProgress(settings(), history(), currentDeclarer, contractWithHands, rngSeed);
    }
}