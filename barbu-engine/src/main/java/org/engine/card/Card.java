package org.engine.card;



/**
 * Immutable value type representing a single playing card.
 *
 * <p>A card is uniquely identified by its {@link Suit} and {@link Rank}.
 */
public record Card(Suit suit, Rank rank) {
    @Override
    public String toString() {
        return rank.toString() + suit.toString();
    }
}
