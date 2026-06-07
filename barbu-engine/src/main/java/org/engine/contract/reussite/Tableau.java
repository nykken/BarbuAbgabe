package org.engine.contract.reussite;
import org.engine.card.Card;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.PlayArea;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.StringJoiner;

/**
 * A shared card layout consisting of four suit piles, one per suit.
 *
 * <p>Each pile is started by a card of the designated starting rank, then
 * extends downward toward the lowest rank and upward toward Ace. A suit pile
 * that has not yet been started is absent from the tableau.
 *
 * <p>If constructed without a starting rank, the rank of the first card
 * placed determines the starting rank for all piles.
 */
public class Tableau implements PlayArea {

    /**
     * Tracks the current extent of one suit's pile on the tableau.
     *
     * @param low  the lowest rank value placed so far
     * @param high the highest rank value placed so far
     */
    public record SuitPile(Rank low, Rank high) {}

    private final Map<Suit, SuitPile> piles;

    @Nullable
    private final Rank startingRank;

    /**
     * Creates a tableau where the starting rank is determined by the
     * first card placed.
     */
    public Tableau() {
        this(new EnumMap<>(Suit.class), null);
    }

    private Tableau(Map<Suit, SuitPile> piles, @Nullable Rank startingRank) {
        this.piles = Collections.unmodifiableMap(new EnumMap<>(piles));
        this.startingRank = startingRank;
    }

    /**
     * Creates a tableau with a fixed starting rank.
     * Each pile must be started with a card of this rank.
     *
     * @param startingRank the rank that starts each suit pile
     */
    public Tableau(Rank startingRank) {
        this(new EnumMap<>(Suit.class),
                Objects.requireNonNull(startingRank, "startingRank cannot be null — use no-arg constructor for dynamic start"));
    }

    /**
     * Returns whether the given card may legally be placed on the tableau.
     *
     * <p>A card is legal if:
     * <ul>
     *   <li>no piles exist yet and the starting rank is dynamic, or</li>
     *   <li>its suit pile has not been started and its rank matches the
     *       starting rank, or</li>
     *   <li>its suit pile exists and its rank is exactly one step beyond
     *       the current low or high end of that pile.</li>
     * </ul>
     *
     * @param card the card to check
     * @return {@code true} if the card may be placed
     */
    public boolean isLegal(Card card) {
        if (startingRank == null) {
            return true;
        }

        SuitPile pile = piles.get(card.suit());
        Rank rank = card.rank();

        if (pile == null) {
            return rank == startingRank;
        }

        return rank.isPredecessorOf(pile.low()) || rank.isSuccessorOf(pile.high());
    }

    /**
     * Returns a new tableau with the given card placed.
     *
     * @param card the card to place
     * @throws IllegalArgumentException if the card is not a legal placement
     */
    public Tableau with(Card card) {
        if (!isLegal(card)) {
            throw new IllegalArgumentException(card + " is not a legal placement on the tableau");
        }

        Rank rank = card.rank();

        // If no starting rank has been set yet, set the card's rank as the starting rank
        Rank nextStartingRank = startingRank == null ? rank : startingRank;

        EnumMap<Suit, SuitPile> nextPiles = new EnumMap<>(Suit.class);
        nextPiles.putAll(piles);
        nextPiles.compute(card.suit(), (suit, pile) -> {
            if (pile == null) {
                return new SuitPile(rank, rank);
            }
            if (rank.isPredecessorOf(pile.low())) {
                return new SuitPile(rank, pile.high());
            }
            return new SuitPile(pile.low(), rank);
        });
        return new Tableau(nextPiles, nextStartingRank);
    }

    /**
     * Returns the current state of all suit piles.
     * Suits whose starting card has not been played yet are absent from the map.
     *
     * @return an unmodifiable view of the tableau's suit piles
     */
    public Map<Suit, SuitPile> getPiles() {
        return Collections.unmodifiableMap(piles);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("  ");
        for (Suit suit : Suit.values()) {
            SuitPile pile = piles.get(suit);
            String range = (pile == null) ? "—"
                    : (pile.low() == pile.high())
                    ? pile.low().toString()
                    : pile.low() + "→" + pile.high();
            sj.add(suit + ":" + range);
        }
        return sj.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tableau other
                && Objects.equals(startingRank, other.startingRank)
                && piles.equals(other.piles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startingRank, piles);
    }
}
