package org.engine.contract.trick;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.ContractState;
import org.engine.contract.IllegalMoveException;
import org.engine.contract.PlayArea;
import org.engine.game.Move;
import org.engine.game.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-progress state for a trick-taking contract.
 *
 * <p>Instances are created only by {@link TrickTakingContract#start} and
 * by {@link #applyMove} as each card is played.
 */
public class TrickTakingState extends ContractState {

    private final TrickTakingContract trickContract;
    private final Player currentPlayer;
    private final Map<Player, Hand> hands;
    private final Trick currentTrick;
    private final List<Trick> playedTricks;
    private final Map<Player, Set<Card>> impossibleCards;

    TrickTakingState(TrickTakingContract contract, Player currentPlayer, Map<Player, Hand> hands) {
        this(contract, currentPlayer, hands, new Trick(), List.of(), emptyImpossible());
    }

    /** Package-private: constructed by {@link TrickTakingContract#start} and internally. */
    TrickTakingState(TrickTakingContract contract, Player currentPlayer,
                     Map<Player, Hand> hands, Trick currentTrick, List<Trick> playedTricks,
                     Map<Player, Set<Card>> impossibleCards) {
        super(contract);
        this.trickContract = contract;
        this.currentPlayer = currentPlayer;
        this.hands = Map.copyOf(new EnumMap<>(hands));
        this.currentTrick = currentTrick;
        this.playedTricks = List.copyOf(playedTricks);
        this.impossibleCards = impossibleCards;
    }

    // ~~~~~~~~~~ CURRENT STATE ~~~~~~~~~~

    @Override
    public ContractState withHands(Map<Player, Hand> newHands) {
        return new TrickTakingState(trickContract, currentPlayer, newHands, currentTrick, playedTricks, impossibleCards);
    }

    @Override
    public Map<Player, Set<Card>> impossibleCards() {
        return impossibleCards;
    }

    public List<Trick> playedTricks() {
        return playedTricks;
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
    public Trick playArea() {
        return currentTrick;
    }

    @Override
    public Set<Card> currentPlayerLegalCards() {
        Set<Card> hand = hands.get(currentPlayer).cards();

        if (currentTrick.isEmpty()) {
            return trickContract.leadRestriction().restrictLeads(hand, playedTricks);
        }

        Suit led = currentTrick.ledSuit();
        Set<Card> inSuit = hand.stream()
                .filter(c -> c.suit() == led)
                .collect(Collectors.toUnmodifiableSet());
        return inSuit.isEmpty() ? hand : inSuit;
    }

    @Override
    public List<Move> legalMoves() {
        return currentPlayerLegalCards().stream()
                .<Move>map(c -> new Move.PlayCard(currentPlayer, c))
                .toList();
    }

    @Override
    public boolean isFinished() {
        return currentTrick.isEmpty() && trickContract.scoringPolicy().isFinished(hands);
    }

    @Override
    protected Map<Player, Integer> calculateScores() {
        return trickContract.scoringPolicy().score(playedTricks);
    }

    // ~~~~~~~~~~ MOVE ~~~~~~~~~~

    @Override
    protected ContractState doApplyMove(Move move) {
        return switch (move) {
            case Move.PlayCard(var player, var card) -> applyPlay(player, card);
            default -> throw new IllegalMoveException(contract().getClass().getSimpleName()
                    + " does not support: "
                    + move.getClass().getSimpleName());
        };
    }

    private ContractState applyPlay(Player player, Card card) {
        requireCurrentPlayer(currentPlayer, player);

        if (!currentPlayerLegalCards().contains(card)) {
            throw new IllegalMoveException(card + " is not a legal play for " + player);
        }

        Hand newHand = hands.get(player).withoutCard(card);
        Map<Player, Hand> newHands = withUpdatedHand(hands, player, newHand);
        Trick newTrick = currentTrick.with(player, card);
        Map<Player, Set<Card>> newImpossible = computeNewImpossible(player, card);

        if (newTrick.isComplete()) {
            Player winner = newTrick.winner();
            List<Trick> newPlayedTricks = Stream.concat(playedTricks.stream(), Stream.of(newTrick)).toList();
            return new TrickTakingState(trickContract, winner, newHands, new Trick(), newPlayedTricks, newImpossible);
        } else {
            return new TrickTakingState(trickContract, player.next(), newHands, newTrick, playedTricks, newImpossible);
        }
    }

    private Map<Player, Set<Card>> computeNewImpossible(Player player, Card card) {
        Map<Player, Set<Card>> updated = copyImpossible(impossibleCards);
        if (!currentTrick.isEmpty() && card.suit() != currentTrick.ledSuit()) {
            Suit led = currentTrick.ledSuit();
            for (Rank r : Rank.values()) {
                updated.get(player).add(new Card(led, r));
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

    /**
     *  Returns a map of how many tricks each player has won so far in this contract.
     */
    public Map<Player, Integer> tricksTaken() {
        Map<Player, Integer> counts = new EnumMap<>(Player.class);
        for (Player player : Player.values()) {
            counts.put(player, 0);
        }

        for (Trick trick : playedTricks) {
            Player winner = trick.winner();
            counts.put(winner, counts.get(winner) + 1);
        }

        return Map.copyOf(counts);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        TrickTakingState other = (TrickTakingState) o;
        return currentPlayer == other.currentPlayer
                && hands.equals(other.hands)
                && currentTrick.equals(other.currentTrick)
                && playedTricks.equals(other.playedTricks);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), currentPlayer, hands, currentTrick, playedTricks);
    }
}