package org.engine.game;

import org.engine.contract.Contract;
import org.engine.contract.ContractState;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.engine.helpers.TestContracts.*;
import static org.engine.helpers.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class HistoryTest {

    private static final Contract TRICKS = tricks(5);

    /**
     * Plays a TRICKS contract to completion with NORTH declaring.
     * NORTH wins the only trick (AS beats KS/QS/JS) → NORTH scores 5 pts.
     */
    
    private static ContractState northWinsTricks() {
        ContractState state = TRICKS.start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
        state = state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));
        state = state.applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
        state = state.applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));
        return state.applyMove(new Move.PlayCard(Player.WEST,   c("JS")));
    }

    /**
     * Plays a TRICKS contract to completion with EAST declaring.
     * EAST leads KS; NORTH wins (AS highest spade) → NORTH scores 5 pts.
     */
    
    private static ContractState eastDeclaresNorthWins() {
        ContractState state = TRICKS.start(Player.EAST, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
        state = state.applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
        state = state.applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));
        state = state.applyMove(new Move.PlayCard(Player.WEST,  c("JS")));
        return state.applyMove(new Move.PlayCard(Player.NORTH,  c("AS")));
    }

    // -------------------------------------------------------------------------
    // Empty history
    // -------------------------------------------------------------------------

    @Nested
    class EmptyHistory {

        @Test
        void hasNoFinishedContracts() {
            for (Player p : Player.values()) {
                assertTrue(History.create().finishedContractsForDeclarer(p).isEmpty());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Immutability and accumulation
    // -------------------------------------------------------------------------

    @Nested
    class WithContracts {

        @Test
        void withReturnsNewInstance() {
            History original = History.create();
            History updated = original.with(Player.NORTH, northWinsTricks());
            assertNotSame(original, updated);
        }

        @Test
        void withDoesNotMutateOriginal() {
            History original = History.create();
            original.with(Player.NORTH, northWinsTricks());
            assertTrue(original.finishedContractsForDeclarer(Player.NORTH).isEmpty());
        }

        @Test
        void withAppendsContractsForSameDeclarer() {
            History history = History.create()
                    .with(Player.NORTH, northWinsTricks())
                    .with(Player.NORTH, northWinsTricks());
            assertEquals(2, history.finishedContractsForDeclarer(Player.NORTH).size());
        }

        @Test
        void cumulativeScoresSumsAcrossAllDeclarersAndContracts() {
            // Both rounds won by NORTH (5 pts each) regardless of who declared
            History history = History.create()
                    .with(Player.NORTH, northWinsTricks())    // NORTH declares, NORTH wins → +5
                    .with(Player.EAST,  eastDeclaresNorthWins()); // EAST declares, NORTH wins → +5

            assertEquals(10, history.cumulativeScores().get(Player.NORTH));
            assertEquals(0,  history.cumulativeScores().get(Player.EAST));
            assertEquals(0,  history.cumulativeScores().get(Player.SOUTH));
            assertEquals(0,  history.cumulativeScores().get(Player.WEST));
        }
    }

}
