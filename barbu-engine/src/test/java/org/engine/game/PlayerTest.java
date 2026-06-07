package org.engine.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void nextAdvancesInOrder() {
        assertEquals(Player.EAST,  Player.NORTH.next());
        assertEquals(Player.SOUTH, Player.EAST.next());
        assertEquals(Player.WEST,  Player.SOUTH.next());
    }

    @Test
    void nextWrapsAroundFromLast() {
        assertEquals(Player.NORTH, Player.WEST.next());
    }

    @Test
    void previousReverseOrder() {
        assertEquals(Player.NORTH, Player.EAST.previous());
        assertEquals(Player.EAST,  Player.SOUTH.previous());
        assertEquals(Player.SOUTH, Player.WEST.previous());
    }

    @Test
    void previousWrapsAroundFromFirst() {
        assertEquals(Player.WEST, Player.NORTH.previous());
    }

    @Test
    void nextAndPreviousAreInverse() {
        for (Player p : Player.values()) {
            assertEquals(p, p.next().previous());
            assertEquals(p, p.previous().next());
        }
    }
}
