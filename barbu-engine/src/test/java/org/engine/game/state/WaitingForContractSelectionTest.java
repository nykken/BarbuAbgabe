package org.engine.game.state;

import org.engine.card.Hand;
import org.engine.contract.Contract;
import org.engine.contract.ContractState;
import org.engine.contract.IllegalMoveException;
import org.engine.game.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.engine.helpers.TestContracts.*;
import static org.engine.helpers.TestHelper.*;
import static org.engine.helpers.TestSettings.of;
import static org.junit.jupiter.api.Assertions.*;

class WaitingForContractSelectionTest {

    private static final Contract TRICKS = tricks(5);
    private static final Contract QUEENS = queens(10);

    private static final GameSettings ONE_CONTRACT   = of(TRICKS);
    private static final GameSettings TWO_CONTRACTS  = of(TRICKS, QUEENS);

    /** One card per player — enough for a single playable trick. */
    private static Map<Player, Hand> oneCardHands() {
        return handsOf(h("AS"), h("KS"), h("QS"), h("JS"));
    }

    /** Plays a TRICKS contract to completion (one trick, NORTH wins). */
    private static ContractState finishedTricksContract() {
        ContractState state = TRICKS.start(Player.NORTH, oneCardHands());
        state = state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));
        state = state.applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
        state = state.applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));
        return state.applyMove(new Move.PlayCard(Player.WEST,  c("JS")));
    }

    // -------------------------------------------------------------------------
    // Contract selection transitions
    // -------------------------------------------------------------------------

    @Nested
    class ContractSelection {

        @Test
        void selectingValidContractTransitionsToContractInProgress() {
            WaitingForContractSelection state = new WaitingForContractSelection(
                    ONE_CONTRACT, History.create(), Player.NORTH, 42L);

            GameState next = state.applyMove(new Move.SelectContract(Player.NORTH, TRICKS));

            assertInstanceOf(ContractInProgress.class, next);
        }

        @Test
        void selectedContractIsActiveInNextState() {
            WaitingForContractSelection state = new WaitingForContractSelection(
                    TWO_CONTRACTS, History.create(), Player.NORTH, 42L);

            ContractInProgress next = (ContractInProgress) state.applyMove(new Move.SelectContract(Player.NORTH, QUEENS));

            assertEquals(QUEENS, next.activeContract().contract());
        }

        @Test
        void currentDeclarerCarriedOverToContractInProgress() {
            WaitingForContractSelection state = new WaitingForContractSelection(
                    ONE_CONTRACT, History.create(), Player.EAST, 42L);

            ContractInProgress next = (ContractInProgress) state.applyMove(new Move.SelectContract(Player.EAST, TRICKS));

            assertEquals(Player.EAST, next.currentDeclarer());
        }
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Nested
    class Validation {

        @Test
        void selectingAlreadyPlayedContractThrows() {
            History history = History.create().with(Player.NORTH, finishedTricksContract());
            WaitingForContractSelection state = new WaitingForContractSelection(
                    TWO_CONTRACTS, history, Player.NORTH, 42L);

            assertThrows(IllegalMoveException.class,
                    () -> state.applyMove(new Move.SelectContract(Player.NORTH, TRICKS)));
        }

        @Test
        void selectingContractNotInSettingsThrows() {
            Contract outsider = hearts(5, 2); // "HEARTS" not in ONE_CONTRACT
            WaitingForContractSelection state = new WaitingForContractSelection(
                    ONE_CONTRACT, History.create(), Player.NORTH, 42L);

            assertThrows(IllegalMoveException.class,
                    () -> state.applyMove(new Move.SelectContract(Player.NORTH, outsider)));
        }

        @Test
        void playCardMoveThrows() {
            WaitingForContractSelection state = new WaitingForContractSelection(
                    ONE_CONTRACT, History.create(), Player.NORTH, 42L);

            assertThrows(IllegalStateException.class,
                    () -> state.applyMove(new Move.PlayCard(Player.NORTH, c("AS"))));
        }

        @Test
        void passMoveThrows() {
            WaitingForContractSelection state = new WaitingForContractSelection(
                    ONE_CONTRACT, History.create(), Player.NORTH, 42L);

            assertThrows(IllegalStateException.class,
                    () -> state.applyMove(new Move.Pass(Player.NORTH)));
        }
    }
}