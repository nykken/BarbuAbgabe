package org.engine.game.state;

import org.engine.card.Hand;
import org.engine.contract.Contract;
import org.engine.game.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.engine.helpers.TestContracts.*;
import static org.engine.helpers.TestHelper.*;
import static org.engine.helpers.TestSettings.of;
import static org.junit.jupiter.api.Assertions.*;

class ContractInProgressTest {

    private static final Contract TRICKS = tricks(5);
    private static final Contract QUEENS = queens(10);

    private static final GameSettings ONE_CONTRACT  = of(TRICKS);
    private static final GameSettings TWO_CONTRACTS = of(TRICKS, QUEENS);

    /** One card per player — plays exactly one trick then the contract is finished. */
    private static Map<Player, Hand> oneCardHands() {
        return handsOf(h("AS"), h("KS"), h("QS"), h("JS"));
    }

    /** Two cards per player — plays exactly two tricks. */
    private static Map<Player, Hand> twoCardHands() {
        return handsOf(h("AS", "2C"), h("KS", "3C"), h("QS", "4C"), h("JS", "5C"));
    }

    /**
     * Plays a full trick starting from NORTH.
     * NORTH holds AS (highest spade) so NORTH wins.
     */
    private static GameState playOneTrick(ActiveGameState state) {
        GameState s = state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));
        s = ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
        s = ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));
        return ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.WEST,   c("JS")));
    }

    // -------------------------------------------------------------------------
    // Move application
    // -------------------------------------------------------------------------

    @Nested
    class MoveApplication {

        @Test
        void intermediatePlayProducesNewContractInProgress() {
            ContractInProgress state = new ContractInProgress(
                    ONE_CONTRACT, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, twoCardHands()), 42L);

            GameState next = state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));

            assertInstanceOf(ContractInProgress.class, next);
        }

        @Test
        void intermediatePlayAdvancesCurrentPlayer() {
            ContractInProgress state = new ContractInProgress(
                    ONE_CONTRACT, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, twoCardHands()), 42L);

            ContractInProgress next = (ContractInProgress) state.applyMove(new Move.PlayCard(Player.NORTH, c("AS")));

            assertEquals(Player.EAST, next.activeContract().currentPlayer());
        }
    }

    // -------------------------------------------------------------------------
    // Contract completion — state machine transitions
    // -------------------------------------------------------------------------

    @Nested
    class ContractCompletion {

        @Test
        void whenDeclarerHasMoreContracts_transitionsToWaitingForSameDeclarer() {
            // TWO_CONTRACTS: after NORTH finishes TRICKS, QUEENS still remains
            ContractInProgress state = new ContractInProgress(
                    TWO_CONTRACTS, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, oneCardHands()), 42L);

            GameState next = playOneTrick(state);

            assertInstanceOf(WaitingForContractSelection.class, next);
            assertEquals(Player.NORTH, ((WaitingForContractSelection) next).currentDeclarer());
        }

        @Test
        void whenDeclarerFinishesAllContracts_advancesToNextDeclarer() {
            // ONE_CONTRACT: NORTH finishing is NORTH's only contract → advance to EAST
            ContractInProgress state = new ContractInProgress(
                    ONE_CONTRACT, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, oneCardHands()), 42L);

            GameState next = playOneTrick(state);

            assertInstanceOf(WaitingForContractSelection.class, next);
            assertEquals(Player.EAST, ((WaitingForContractSelection) next).currentDeclarer());
        }

        @Test
        void whenLastDeclarerFinishes_transitionsToGameOver() {
            // WEST is the last declarer: WEST.next() == NORTH == STARTING_PLAYER → GameOver
            // WEST is declarer so WEST plays first (JS), then NORTH (AS) wins the trick
            ContractInProgress state = new ContractInProgress(
                    ONE_CONTRACT, History.create(), Player.WEST,
                    TRICKS.start(Player.WEST, oneCardHands()), 42L);

            GameState s = state.applyMove(new Move.PlayCard(Player.WEST,  c("JS")));
            s = ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.NORTH, c("AS")));
            s = ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.EAST,  c("KS")));
            s = ((ActiveGameState) s).applyMove(new Move.PlayCard(Player.SOUTH, c("QS")));

            assertInstanceOf(GameOver.class, s);
        }

        @Test
        void finishedContractIsRecordedInHistory() {
            ContractInProgress state = new ContractInProgress(
                    TWO_CONTRACTS, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, oneCardHands()), 42L);

            GameState next = playOneTrick(state);

            List<History.FinishedContract> recorded = next.history().finishedContractsForDeclarer(Player.NORTH);
            assertEquals(1, recorded.size());
            assertEquals(TRICKS, recorded.getFirst().contract());
        }

        @Test
        void nextDealIsDeterministicFromSeed() {
            long seed = 42L;
            ContractInProgress s1 = new ContractInProgress(
                    TWO_CONTRACTS, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, oneCardHands()), seed);
            ContractInProgress s2 = new ContractInProgress(
                    TWO_CONTRACTS, History.create(), Player.NORTH,
                    TRICKS.start(Player.NORTH, oneCardHands()), seed);

            WaitingForContractSelection next1 = (WaitingForContractSelection) playOneTrick(s1);
            WaitingForContractSelection next2 = (WaitingForContractSelection) playOneTrick(s2);

            for (Player p : Player.values()) {
                assertEquals(next1.hands().get(p).cards(), next2.hands().get(p).cards(),
                        "Expected same cards for " + p + " from identical seeds");
            }
        }
    }
}