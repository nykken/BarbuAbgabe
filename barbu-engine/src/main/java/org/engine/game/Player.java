package org.engine.game;


/**
 * Represents the four players in the game, ordered clockwise.
 * <p>
 * Supports circular traversal via {@link #next()} and {@link #previous()},
 * so the sequence wraps around seamlessly (e.g. {@code WEST.next() == NORTH}).
 * </p>
 */
public enum Player {
    NORTH, EAST, SOUTH, WEST;

    /** The player who by convention starts as the first declarer.
     * Used to determine when a full rotation of the table has completed.
     */
    public static final Player STARTING_PLAYER = NORTH;

    private static final Player[] VALUES = values();

    /** Returns the next player clockwise, wrapping from WEST back to NORTH. */
    public Player next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** Returns the previous player counter-clockwise, wrapping from NORTH back to WEST. */
    public Player previous() {
        return VALUES[(ordinal() - 1 + VALUES.length) % VALUES.length];
    }
}