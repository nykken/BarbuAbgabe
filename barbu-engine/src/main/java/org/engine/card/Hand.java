package org.engine.card;


import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;


/**
 * Represents a player's hand of cards.
 * Cards are unordered and unique.
 */
final public class Hand {
    private final Set<Card> cards;

    /**
     * Creates a hand containing the given cards. Duplicate cards are silently
     * deduplicated; the collection is copied and not held by reference.
     *
     * @param cards the cards for this hand
     */
    public Hand(Collection<Card> cards) {
        this.cards = new HashSet<>(cards);
    }

    /**
     * Returns an unmodifiable view of all cards in this hand.
     *
     * @return an unmodifiable set of cards; never {@code null}
     */
    public Set<Card> cards() {
        return Collections.unmodifiableSet(cards);
    }

    /**
     * Returns all cards of the given suit in this hand.
     *
     * @param suit the suit to filter by
     * @return an unmodifiable set of matching cards, empty if none
     */
    public Set<Card> getCardsOfSuit(Suit suit) {
        return cards.stream()
                .filter(c -> c.suit() == suit)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns whether this hand contains at least one card of the given suit.
     *
     * @param suit the suit to check
     */
    public boolean hasSuit(Suit suit) {
        return cards.stream().anyMatch(c -> c.suit() == suit);
    }

    /**
     * Returns whether this hand contains at least one card of the given rank.
     *
     * @param rank the rank to check
     */
    public boolean hasRank(Rank rank) {
        return cards.stream().anyMatch(c -> c.rank() == rank);
    }

    /**
     * Returns a new hand with the given card removed.
     *
     * @param card the card to remove
     * @throws IllegalArgumentException if the card is not in this hand
     */
    public Hand withoutCard(Card card) {
        if (!cards.contains(card)) {
            throw new IllegalArgumentException(
                    "Card %s is not in this hand".formatted(card)
            );
        }
        Set<Card> next = new HashSet<>(cards);
        next.remove(card);
        return new Hand(next);
    }


    /**
     * Returns whether this hand contains the given card.
     *
     * @param card the card to look for
     */
    public boolean contains(Card card) {
        return cards.contains(card);
    }


    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public String toString() {
        return cards.stream()
                .sorted(Comparator.comparingInt((Card c) -> c.suit().ordinal())
                        .thenComparingInt(c -> c.rank().getValue()))
                .map(Card::toString)
                .collect(Collectors.joining(" "));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hand other && cards.equals(other.cards);
    }

    @Override
    public int hashCode() {
        return cards.hashCode();
    }
}
