package org.engine.contract.trick;

import org.engine.card.Card;
import org.engine.card.Suit;
import org.engine.contract.PlayArea;
import org.engine.game.Player;

import java.util.*;

/**
 * An immutable record of one trick: the cards played so far and the order they were played.
 *
 * <p>A trick starts empty, grows one card at a time via {@link #with}, and is complete
 * once all four players have played. The winner is the player who played the highest card
 * of the led suit.
 */
public class Trick implements PlayArea {
    private record TrickCard(Player player, Card card) {}

    private final List<TrickCard> cards;

    public Trick() {
        this.cards = List.of();
    }

    private Trick(List<TrickCard> cards) {
        this.cards = List.copyOf(cards);
    }

    /**
     * Returns a new trick instance with the given card added for the specified player.
     * This operation does not mutate the current instance.
     *
     * @param player the player making the move
     * @param card   the card being played
     * @return a new {@code Trick} instance containing the appended turn
     * @throws IllegalStateException    if the trick is already complete
     * @throws IllegalArgumentException if this player has already played in this trick
     */
    public Trick with(Player player, Card card) {
        if (isComplete()) {
            throw new IllegalStateException("Trick is already complete");
        }
        if (cardPlayedBy(player).isPresent()) {
            throw new IllegalArgumentException(
                    "Player %s has already played in this trick".formatted(player)
            );
        }
        List<TrickCard> next = new ArrayList<>(cards);
        next.add(new TrickCard(player, card));
        return new Trick(next);
    }

    /**
     * Returns the leading suit in this trick
     *
     * @throws IllegalStateException if no cards have been played yet
     */
    public Suit ledSuit() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Trick has no cards yet");
        }
        return cards.getFirst().card().suit();
    }

    public boolean isComplete() {
        return cards.size() == Player.values().length;
    }


    public boolean contains(Card card) {
        return cards.stream()
                .anyMatch(tc -> tc.card().equals(card));
    }

    /**
     * Retrieves the card played by a specific player in this trick, if any.
     *
     * @param player the player whose card to retrieve
     * @return an {@link Optional} containing the card played by the player,
     * or an empty Optional if they haven't played yet
     */
    public Optional<Card> cardPlayedBy(Player player) {
        return cards.stream()
                .filter(tc -> tc.player() == player)
                .map(TrickCard::card)
                .findFirst();
    }

    public List<Card> cardsOfSuit(Suit suit) {
        return cards.stream()
                .map(TrickCard::card)
                .filter(card -> card.suit() == suit)
                .toList();
    }

    /**
     * Returns the player who won this trick.
     *
     * @throws IllegalStateException if the trick is not yet complete
     */
    public Player winner() {
        if (!isComplete()) {
            throw new IllegalStateException(
                    "Cannot determine winner of incomplete trick"
            );
        }
        Suit led = ledSuit();
        return cards.stream()
                .filter(tc -> tc.card().suit() == led)
                .max(Comparator.comparingInt(tc -> tc.card().rank().getValue()))
                .map(TrickCard::player)
                .orElseThrow();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public List<Player> playOrder() {
        return cards.stream().map(TrickCard::player).toList();
    }

    public List<Card> cards() {
        return cards.stream()
                .map(TrickCard::card)
                .toList();
    }

    /** e.g. {@code "NORTH:JH  EAST:QS  SOUTH:—  WEST:—"} */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("  ");
        for (Player player : Player.values()) {
            sj.add(player + ":" + cardPlayedBy(player).map(Card::toString).orElse("—"));
        }
        return sj.toString();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Trick other && cards.equals(other.cards);
    }

    @Override
    public int hashCode() {
        return cards.hashCode();
    }
}