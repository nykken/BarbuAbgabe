package org.engine.game.state;

import org.engine.card.Hand;
import org.engine.contract.Contract;
import org.engine.contract.ContractState;
import org.engine.contract.IllegalMoveException;
import org.engine.game.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Game state while the current declarer is choosing which contract to play.
 */
public final class WaitingForContractSelection extends ActiveGameState {
    private final Player currentDeclarer;
    private final Map<Player, Hand> hands;
    private final long nextSeed;

    WaitingForContractSelection(GameSettings settings, History history,
                                Player currentDeclarer, long rngSeed) {
        super(settings, history);
        this.currentDeclarer = Objects.requireNonNull(currentDeclarer);
        Random rng = new Random(rngSeed);
        this.hands = Map.copyOf(new EnumMap<>(CardDealer.deal(settings.deckVariant(), rng)));
        this.nextSeed = rng.nextLong();
    }

    // Private constructor to allow passing determinized hands.
    private WaitingForContractSelection(GameSettings settings, History history,
                                        Player currentDeclarer, Map<Player, Hand> hands, long nextSeed) {
        super(settings, history);
        this.currentDeclarer = currentDeclarer;
        this.hands = Map.copyOf(hands);
        this.nextSeed = nextSeed;
    }

    @Override
    public Player currentDeclarer() { return currentDeclarer; }

    @Override
    public Map<Player, Hand> hands() { return hands; }

    @Override
    public Player currentPlayer() { return currentDeclarer; }

    @Override
    public List<Move> legalMoves() {
        return remainingContracts(currentDeclarer).stream()
                .map(c -> (Move) new Move.SelectContract(currentDeclarer, c))
                .toList();
    }

    @Override
    public GameState applyMove(Move move) {
        if (!(move instanceof Move.SelectContract m)) {
            throw new IllegalStateException(
                    "Expected SelectContract but got " + move.getClass().getSimpleName());
        }
        if (m.declarer() != currentDeclarer) {
            throw new IllegalMoveException(
                    m.declarer() + " cannot select a contract. Current declarer is " + currentDeclarer);
        }
        Contract chosen = m.contract();
        if (!remainingContracts(currentDeclarer).contains(chosen)) {
            throw new IllegalMoveException(
                    "Selected contract is not available for " + currentDeclarer);
        }
        ContractState active = chosen.start(currentDeclarer, hands);
        return new ContractInProgress(settings(), history(), currentDeclarer, active, nextSeed);
    }

    @Override
    public ActiveGameState withHands(Map<Player, Hand> newHands) {
        return new WaitingForContractSelection(settings(), history(), currentDeclarer, newHands, nextSeed);
    }
}