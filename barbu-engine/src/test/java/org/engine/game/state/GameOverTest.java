package org.engine.game.state;

import org.engine.contract.Contract;
import org.engine.contract.ContractState;
import org.engine.game.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.engine.helpers.TestContracts.*;
import static org.engine.helpers.TestHelper.*;
import static org.engine.helpers.TestSettings.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameOverTest {

    private static final Contract TRICKS = tricks(5);
    private static final GameSettings SETTINGS = of(TRICKS);

    /** NORTH wins the only trick — scores 5 points for TRICKS. */
    private static ContractState northWinsTrick() {
        ContractState state = TRICKS.start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
        state = state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));
        state = state.applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
        state = state.applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));
        return state.applyMove(new Move.PlayCard(Player.WEST,   c("JS")));
    }

    // -------------------------------------------------------------------------
    // Scores
    // -------------------------------------------------------------------------

    @Nested
    class Scores {

        @Test
        void cumulativeScoresAreZeroWithEmptyHistory() {
            GameOver state = new GameOver(SETTINGS, History.create());

            for (Player p : Player.values()) {
                assertEquals(0, state.cumulativeScores().get(p));
            }
        }

        @Test
        void cumulativeScoresReflectHistory() {
            // NORTH wins the one trick → 5 points for NORTH, 0 for everyone else
            History history = History.create().with(Player.NORTH, northWinsTrick());
            GameOver state = new GameOver(SETTINGS, history);

            assertEquals(5, state.cumulativeScores().get(Player.NORTH));
            assertEquals(0, state.cumulativeScores().get(Player.EAST));
            assertEquals(0, state.cumulativeScores().get(Player.SOUTH));
            assertEquals(0, state.cumulativeScores().get(Player.WEST));
        }
    }
}