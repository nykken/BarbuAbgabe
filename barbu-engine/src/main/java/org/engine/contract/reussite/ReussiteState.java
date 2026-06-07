package org.engine.contract.reussite;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.ContractState;
import org.engine.contract.IllegalMoveException;
import org.engine.contract.PlayArea;
import org.engine.game.Move;
import org.engine.game.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-progress state for a Réussite contract.
 *
 * <p>Instances are created by {@link ReussiteContract#start} and
 * by {@link #applyMove} after each valid card play or pass.
 */
public class ReussiteState extends ContractState {
    private final ReussiteContract reussiteContract;
    private final Player currentPlayer;
    private final Map<Player, Hand> hands;
    private final Tableau tableau;
    private final List<Player> finishOrder;
    private final Map<Player, Set<Card>> impossibleCards;


    /** Package-private: constructed by {@link ReussiteContract#start} and internally. */
    ReussiteState(ReussiteContract contract, Player currentPlayer,
                  Map<Player, Hand> hands, Tableau tableau, List<Player> finishOrder) {
        this(contract, currentPlayer, hands, tableau, finishOrder, emptyImpossible());
    }

    ReussiteState(ReussiteContract contract, Player currentPlayer,
                  Map<Player, Hand> hands, Tableau tableau, List<Player> finishOrder,
                  Map<Player, Set<Card>> impossibleCards) {
        super(contract);
        this.reussiteContract = contract;
        this.currentPlayer = currentPlayer;
        this.hands = Map.copyOf(new EnumMap<>(hands));
        this.tableau = tableau;
        this.finishOrder = List.copyOf(finishOrder);
        this.impossibleCards = impossibleCards;
    }

    // ~~~~~~~~~~ CURRENT STATE ~~~~~~~~~~

    @Override
    public ContractState withHands(Map<Player, Hand> newHands) {
        return new ReussiteState(reussiteContract, currentPlayer, newHands, tableau, finishOrder, impossibleCards);
    }

    @Override
    public Map<Player, Set<Card>> impossibleCards() {
        return impossibleCards;
    }

    @Override
    public Player currentPlayer() {
        return currentPlayer;
    }

    @Override
    public Map<Player, Hand> hands() {
        return hands;
    }

    @Override
    public Tableau playArea() {
        return tableau;
    }

    @Override
    public Set<Card> currentPlayerLegalCards() {
        return hands.get(currentPlayer).cards().stream()
                .filter(tableau::isLegal)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<Move> legalMoves() {

        Set<Card> cards = currentPlayerLegalCards();
        if (cards.isEmpty()) return List.of(new Move.Pass(currentPlayer));
        return cards.stream()
                .<Move>map(c -> new Move.PlayCard(currentPlayer, c))
                .toList();
    }

    @Override
    public boolean isFinished() {
        return finishOrder.size() >= finishThreshold();
    }

    @Override
    protected Map<Player, Integer> calculateScores() {
        Map<Player, Integer> scores = new EnumMap<>(Player.class);
        List<Integer> placementPoints = reussiteContract.placementPoints();

        for (int i = 0; i < finishOrder.size(); i++) {
            int points = i < placementPoints.size() ? placementPoints.get(i) : 0;
            scores.put(finishOrder.get(i), points);
        }

        for (Player player : Player.values()) {
            scores.putIfAbsent(player, 0);
        }

        return Collections.unmodifiableMap(scores);
    }



    // ~~~~~~~~~~ MOVE ~~~~~~~~~~

    @Override
    protected ContractState doApplyMove(Move move) {
        return switch (move) {
            case Move.PlayCard(var player, var card) -> applyPlay(player, card);
            case Move.Pass(var player)               -> applyPass(player);
            default -> throw new IllegalMoveException(contract().getClass().getSimpleName() + " does not support: " + move.getClass().getSimpleName());
        };
    }

    /**
     * Applies a card play. If the played card's rank matches {@link ReussiteContract#replayAfterRank},
     * the same player gets an immediate extra turn instead of passing to the next seat.
     * If enough players have finished after this play, the contract ends immediately without
     * advancing the turn.
     */
    private ContractState applyPlay(Player player, Card card) {
        requireCurrentPlayer(currentPlayer, player);

        Set<Card> legal = currentPlayerLegalCards();
        if (!legal.contains(card)) {
            throw new IllegalMoveException(card + " is not a legal play for " + player);
        }

        Hand newHand = hands.get(player).withoutCard(card);
        Map<Player, Hand> newHands = withUpdatedHand(hands, player, newHand);
        Tableau newTableau = tableau.with(card);

        List<Player> newFinishOrder = new ArrayList<>(finishOrder);
        if (newHand.isEmpty()) {
            newFinishOrder.add(player);
        }

        if (newFinishOrder.size() >= finishThreshold()) {
            return new ReussiteState(reussiteContract, currentPlayer, newHands, newTableau, newFinishOrder, impossibleCards);
        }

        boolean replay = reussiteContract.replayAfterRank() != null
                && card.rank() == reussiteContract.replayAfterRank()
                && !newFinishOrder.contains(player);
        Player next = replay ? player : nextActivePlayer(player, newFinishOrder);
        return new ReussiteState(reussiteContract, next, newHands, newTableau, newFinishOrder, impossibleCards);
    }



    private ContractState applyPass(Player player) {
        requireCurrentPlayer(currentPlayer, player);
        if (!currentPlayerLegalCards().isEmpty()) {
            throw new IllegalMoveException(player + " cannot pass with legal moves available.");
        }

        verifyNoDeadlock();

        Player next = nextActivePlayer(player, finishOrder);
        Map<Player, Set<Card>> newImpossible = computeNewImpossible(player);
        return new ReussiteState(reussiteContract, next, hands, tableau, finishOrder, newImpossible);
    }

    private Map<Player, Set<Card>> computeNewImpossible(Player player) {
        Map<Player, Set<Card>> updated = copyImpossible(impossibleCards);
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                Card c = new Card(s, r);
                if (tableau.isLegal(c)) updated.get(player).add(c);
            }
        }
        return Collections.unmodifiableMap(updated);
    }

    private static Map<Player, Set<Card>> emptyImpossible() {
        Map<Player, Set<Card>> map = new EnumMap<>(Player.class);
        for (Player player : Player.values()) map.put(player, new HashSet<>());
        return Collections.unmodifiableMap(map);
    }

    private static Map<Player, Set<Card>> copyImpossible(Map<Player, Set<Card>> source) {
        Map<Player, Set<Card>> copy = new EnumMap<>(Player.class);
        for (Player player : Player.values()) copy.put(player, new HashSet<>(source.get(player)));
        return copy;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** How many players must finish before the contract ends */
    private int finishThreshold() {
        return Math.min(reussiteContract.placementPoints().size(), Player.values().length - 1);
    }

    /** Returns the next player in seat order after {@code from}, skipping anyone already in {@code finishOrder}. */
    private static Player nextActivePlayer(Player from, List<Player> finishOrder) {
        Player next = from.next();
        int attempts = 0;
        while (finishOrder.contains(next)) {
            if (++attempts >= Player.values().length) {
                throw new IllegalStateException("No active players remaining.");
            }
            next = next.next();
        }
        return next;
    }

    /**
     * Sanity check: in Réussite it is mathematically impossible
     * for every active player to be stuck with no legal moves at the same time.
     *<p>
     * If we ever reach a state where nobody can play, it means the engine has a bug.
     * This method exists to catch this bug loudly.
     */
    private void verifyNoDeadlock() {
        EnumSet<Player> active = EnumSet.allOf(Player.class);
        finishOrder.forEach(active::remove);

        boolean anyPlayable = active.stream()
                .flatMap(player -> hands.get(player).cards().stream())
                .anyMatch(tableau::isLegal);

        if (!anyPlayable) {
            throw new IllegalStateException(
                    "Engine Bug: Réussite deadlock detected. " +
                            "No active player has a legal card to play on the current tableau: " + tableau);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        ReussiteState other = (ReussiteState) o;
        return currentPlayer == other.currentPlayer
                && hands.equals(other.hands)
                && tableau.equals(other.tableau)
                && finishOrder.equals(other.finishOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), currentPlayer, hands, tableau, finishOrder);
    }
}