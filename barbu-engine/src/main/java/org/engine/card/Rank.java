package org.engine.card;


/**
 * Represents the rank of a playing card, ordered from {@link #TWO} (value 2) to {@link #ACE} (value 14).
 */
public enum Rank {
    TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6),
    SEVEN(7), EIGHT(8), NINE(9), TEN(10),
    JACK(11), QUEEN(12), KING(13), ACE(14);

    private final int value;

    Rank(int value) {
        this.value = value;
    }

    /** Returns the numeric value of this rank (2 for TWO through 14 for ACE). */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return switch (this) {
            case TWO -> "2"; case THREE -> "3"; case FOUR -> "4"; case FIVE -> "5";
            case SIX -> "6"; case SEVEN -> "7"; case EIGHT -> "8"; case NINE -> "9";
            case TEN -> "10"; case JACK -> "J"; case QUEEN -> "Q"; case KING -> "K";
            case ACE -> "A";
        };
    }

    /**
     * Returns whether this rank is exactly one step above {@code other}.
     *
     * @param other the rank to compare against
     */
    public boolean isSuccessorOf(Rank other) {
        return value == other.value + 1;
    }

    /**
     * Returns whether this rank is exactly one step below {@code other}.
     *
     * @param other the rank to compare against
     */
    public boolean isPredecessorOf(Rank other) {
        return value == other.value - 1;
    }
}
