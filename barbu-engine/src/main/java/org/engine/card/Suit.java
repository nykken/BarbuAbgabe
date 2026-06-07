package org.engine.card;



/**
 * Represents one of the four card suits: {@link #CLUBS}, {@link #DIAMONDS}, {@link #HEARTS}, {@link #SPADES}.
 */
public enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;

    /** Returns the single-character abbreviation: "C", "D", "H", or "S". */
    @Override
    public String toString() {
        return switch (this) {
            case CLUBS -> "C"; case DIAMONDS -> "D"; case HEARTS -> "H"; case SPADES -> "S";
        };
    }
}
