package org.engine.card;

import java.util.*;


/**
 * An ordered, duplicate-free collection of cards that can be shuffled
 * and dealt.
 *
 * <p>Decks are created using one of the predefined configurations
 * in the {@link Variant} enum.
 */
public class Deck {
    private final List<Card> cards;

    private Deck(List<Card> cards) {
        if (cards.size() != Set.copyOf(cards).size()) {
            throw new IllegalArgumentException("Deck contains duplicate cards");
        }
        this.cards = new ArrayList<>(cards);
    }

    /**
     * Creates a deck containing only cards whose rank is at or above
     * the given minimum rank, across all four suits.
     *
     * @param minRank the lowest rank to include in the deck
     */
    private static Deck fromMinRank(Rank minRank) {
        List<Card> cards = Arrays.stream(Suit.values())
                .flatMap(suit -> Arrays.stream(Rank.values())
                        .filter(rank -> rank.getValue() >= minRank.getValue())
                        .map(rank -> new Card(suit, rank)))
                .toList();
        return new Deck(cards);
    }

    /**
     * Shuffles the deck in place using the provided random number generator.
     *
     * @param rng the random number generator to use
     */
    public void shuffle(Random rng) {
        Collections.shuffle(cards, rng);
    }

    /**
     * Shuffles the deck in place using a default random number generator.
     */
    public void shuffle() {
        shuffle(new Random());
    }

    public int size() {
        return cards.size();
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Defines the available deck configurations.
     */
    public enum Variant {
        /** Full 52-card deck (Two through Ace in all four suits). */
        STANDARD(Rank.TWO),
        /** 32-card deck (Seven through Ace in all four suits). */
        FROM_SEVEN(Rank.SEVEN);

        private final Rank minRank;

        /** Returns a new, unshuffled {@link Deck} instance for this variant. */
        Variant(Rank minRank) {
            this.minRank = minRank;
        }

        public Rank minRank() {
            return minRank;
        }

        public Deck create() {
            return Deck.fromMinRank(minRank);
        }
    }
}
