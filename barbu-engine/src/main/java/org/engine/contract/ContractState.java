package org.engine.contract;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.contract.trick.ScoringPolicy;
import org.engine.game.Move;
import org.engine.game.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The in-progress state of a Barbu contract.
 *
 * <p>Created by {@link Contract#start} and updated by {@link #applyMove}.
 * Not mutated in place, every move returns a new instance.
 */
public abstract class ContractState {

    private final Contract contract;

    protected ContractState(Contract contract) {
        this.contract = contract;
    }

    /** The contract governing this state. */
    public Contract contract() {
        return contract;
    }

    public abstract Player currentPlayer();

    public abstract Map<Player, Hand> hands();

    /** The shared play area (trick or tableau). */
    public abstract PlayArea playArea();

    /**
     * Creates a copy of this contract state, replacing the current hands with the provided ones.
     * Used by AI for determinization.
     *
     * @param newHands the hypothetical distribution of cards
     * @return a new state identical to this one, but with the new hands
     */
    public abstract ContractState withHands(Map<Player, Hand> newHands);

    /**
     * Cards each player is provably not holding, derived from observed play history.
     * Updated as moves are applied (off-suit follows in trick-taking; passes in Réussite).
     * Preserved unchanged by {@link #withHands} — constraints are historical, not hand-specific.
     * Used by AI for determinization.
     */
    public abstract Map<Player, Set<Card>> impossibleCards();



    /**
     * Applies {@code move} to this state and returns the resulting state.
     *
     * @param move the move to apply; must be a legal move for the current player
     * @return the new state after the move
     * @throws IllegalMoveException if this contract is already finished
     */
    public final ContractState applyMove(Move move) {
        if (isFinished()) {
            throw new IllegalMoveException("Contract is already finished");
        }
        return doApplyMove(move);
    }

    protected abstract ContractState doApplyMove(Move move);

    /** Returns the cards the current player may legally play. */
    public abstract Set<Card> currentPlayerLegalCards();

    /**
     * Returns the moves the current player may legally make on this turn.
     */
    public abstract List<Move> legalMoves();

    /**
     * Returns {@code true} when this contract has concluded and no further
     * moves are possible.
     */
    public abstract boolean isFinished();

    /** Hook for {@link #scores()}; called only when {@link #isFinished()} is {@code true}. */
    protected abstract Map<Player, Integer> calculateScores();

    /**
     * Returns per-player scores for this contract.
     *
     * @return an unmodifiable map from each player to their score
     * @throws IllegalStateException if the contract is not yet finished
     */
    public Map<Player, Integer> scores() {
        if (!isFinished()) {
            throw new IllegalStateException("Scores are only available after contract is finished");
        }
        return calculateScores();
    }


    // ── helpers ───────────────────────────────────────────────────────────────
    protected static void requireCurrentPlayer(Player expected, Player actual) {
        if (actual != expected) {
            throw new IllegalMoveException("It is not " + actual + "'s turn.");
        }
    }

    protected static Map<Player, Hand> withUpdatedHand(Map<Player, Hand> hands, Player player, Hand newHand) {
        Map<Player, Hand> result = new EnumMap<>(hands);
        result.put(player, newHand);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractState other = (ContractState) o;
        return contract.equals(other.contract);
    }

    @Override
    public int hashCode() {
        return contract.hashCode();
    }
}