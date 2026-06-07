package org.engine.contract;

import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.contract.trick.Trick;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.engine.helpers.TestContracts.*;
import static org.engine.helpers.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class TrickTakingContractTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Play a card for whoever the current player is. */
    private static ContractState play(ContractState contract, String card) {
        return contract.applyMove(new Move.PlayCard(contract.currentPlayer(), c(card)));
    }

    /** Play four cards in current-player order (clears one full trick). */
    private static ContractState playTrick(ContractState contract, String c1, String c2, String c3, String c4) {
        contract = play(contract, c1);
        contract = play(contract, c2);
        contract = play(contract, c3);
        return play(contract, c4);
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Nested
    class InitialState {

        @Test
        void declarerGoesFirst() {
            ContractState contract = tricks(5)
                    .start(Player.SOUTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            assertEquals(Player.SOUTH, contract.currentPlayer());
        }

        @Test
        void notFinishedAtStart() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            assertFalse(contract.isFinished());
        }

        @Test
        void handsMatchDeal() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS", "2H"), h("KS"), h("QS"), h("JS")));
            assertEquals(2, contract.hands().get(Player.NORTH).size());
            assertTrue(contract.hands().get(Player.NORTH).contains(c("AS")));
        }
    }

    // -------------------------------------------------------------------------
    // Playing a card
    // -------------------------------------------------------------------------

    @Nested
    class PlayingACard {

        @Test
        void turnAdvancesToNextPlayerAfterPlay() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            contract = play(contract, "AS");
            assertEquals(Player.EAST, contract.currentPlayer());
        }

        @Test
        void cardRemovedFromHand() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            contract = play(contract, "AS");
            assertFalse(contract.hands().get(Player.NORTH).contains(c("AS")));
        }

        @Test
        void wrongPlayerThrows() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            assertThrows(IllegalMoveException.class,
                    () -> contract.applyMove(new Move.PlayCard(Player.EAST, c("KS"))));
        }

        @Test
        void cardNotInHandThrows() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            assertThrows(IllegalMoveException.class,
                    () -> contract.applyMove(new Move.PlayCard(Player.NORTH, c("2H"))));
        }

        @Test
        void moveOnFinishedContractThrows() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            ContractState finished = contract;
            assertThrows(IllegalMoveException.class,
                    () -> finished.applyMove(new Move.PlayCard(Player.NORTH, c("AS"))));
        }

        @Test
        void unsupportedMoveTypeThrows() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            assertThrows(IllegalMoveException.class,
                    () -> contract.applyMove(new Move.Pass(Player.NORTH)));
        }
    }

    // -------------------------------------------------------------------------
    // Follow-suit enforcement
    // -------------------------------------------------------------------------

    @Nested
    class FollowSuit {

        @Test
        void fullHandLegalWhenLeadingWithNoRestriction() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS", "2H"), h("KS"), h("QS"), h("JS")));
            assertEquals(Set.of(c("AS"), c("2H")), contract.currentPlayerLegalCards());
        }

        @Test
        void mustFollowSuitWhenAble() {
            // East has KS and 2H — after North leads AS (spade), only KS is legal
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS", "2H"), h("QS"), h("JS")));
            contract = play(contract, "AS");
            assertEquals(Set.of(c("KS")), contract.currentPlayerLegalCards());
        }

        @Test
        void canPlayAnythingWhenVoidInLedSuit() {
            // South has only hearts — after North leads AS, full hand is legal
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("2H", "3H"), h("JS")));
            contract = play(contract, "AS");
            contract = play(contract, "KS");
            assertEquals(Set.of(c("2H"), c("3H")), contract.currentPlayerLegalCards());
        }

        @Test
        void offSuitCardDoesNotWinTrick() {
            // North leads AS; East has no spades, plays 2H; South QS, West JS
            // Highest spade is AS — North wins despite East's 2H being numerically lower
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("AS", "2C"), h("2H", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "AS", "2H", "QS", "JS");
            assertEquals(Player.NORTH, contract.currentPlayer());
        }
    }

    // -------------------------------------------------------------------------
    // Trick resolution
    // -------------------------------------------------------------------------

    @Nested
    class TrickResolution {

        @Test
        void trickWinnerLeadsNext() {
            // North leads AS — wins (highest spade)
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("AS", "2C"), h("KS", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            assertEquals(Player.NORTH, contract.currentPlayer());
        }

        @Test
        void lowestLeaderCanLoseToHigherCard() {
            // North leads 2S; East plays AS — East wins and leads next trick
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("2S", "2C"), h("AS", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "2S", "AS", "QS", "JS");
            assertEquals(Player.EAST, contract.currentPlayer());
        }

        @Test
        void playAreaEmptyAfterTrickComplete() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("AS", "2C"), h("KS", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            assertTrue(((Trick) contract.playArea()).isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // isFinished and scores
    // -------------------------------------------------------------------------

    @Nested
    class Completion {

        @Test
        void notFinishedMidTrick() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            contract = play(contract, "AS");
            assertFalse(contract.isFinished());
        }

        @Test
        void notFinishedBetweenTricksWithCardsRemaining() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("AS", "2C"), h("KS", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            assertFalse(contract.isFinished());
        }

        @Test
        void finishedAfterAllTricksPlayed() {
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(h("AS"), h("KS"), h("QS"), h("JS")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            assertTrue(contract.isFinished());
        }

        @Test
        void scoresAccumulateAcrossTricks() {
            // Trick 1: North leads AS → North wins (AS > KS > QS > JS)
            // Trick 2: North leads 2C → West wins (5C > 4C > 3C > 2C)
            // Expected: North=5, East=0, South=0, West=5
            ContractState contract = tricks(5)
                    .start(Player.NORTH, handsOf(
                            h("AS", "2C"), h("KS", "3C"), h("QS", "4C"), h("JS", "5C")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS");
            contract = playTrick(contract, "2C", "3C", "4C", "5C");
            assertEquals(5,  contract.scores().get(Player.NORTH));
            assertEquals(0,  contract.scores().get(Player.EAST));
            assertEquals(0,  contract.scores().get(Player.SOUTH));
            assertEquals(5,  contract.scores().get(Player.WEST));
        }

        @Test
        void heartsScoresCorrect() {
            // HEARTS 5pts/card: North wins trick with 2 hearts → North scores 10
            ContractState contract = hearts(5, 0)
                    .start(Player.NORTH, handsOf(h("AS"), h("2H"), h("3H"), h("2C")));
            contract = playTrick(contract, "AS", "2H", "3H", "2C");
            assertEquals(10, contract.scores().get(Player.NORTH));
            assertEquals(0,  contract.scores().get(Player.EAST));
        }
    }

    // -------------------------------------------------------------------------
    // Lead restriction (opening phase)
    // -------------------------------------------------------------------------

    @Nested
    class HeartLeadRestriction {

        @Test
        void cannotLeadHeartsDuringOpeningPhase() {
            // Opening phase of 2 tricks — AH must not appear in legal leads
            ContractState contract = hearts(5, 2)
                    .start(Player.NORTH, handsOf(
                            h("AS", "AH"), h("KS", "KH"), h("QS", "QH"), h("JS", "JH")));
            assertFalse(contract.currentPlayerLegalCards().contains(c("AH")));
            assertTrue(contract.currentPlayerLegalCards().contains(c("AS")));
        }

        @Test
        void canLeadHeartsAfterOpeningPhase() {
            // Play 2 non-heart tricks, then hearts should be leadable
            ContractState contract = hearts(5, 2)
                    .start(Player.NORTH, handsOf(
                            h("AS", "AC", "AH"),
                            h("KS", "KC", "KH"),
                            h("QS", "QC", "QH"),
                            h("JS", "JC", "JH")));
            contract = playTrick(contract, "AS", "KS", "QS", "JS"); // trick 1
            contract = playTrick(contract, "AC", "KC", "QC", "JC"); // trick 2
            assertTrue(contract.currentPlayerLegalCards().contains(c("AH")));
        }

        @Test
        void onlyRestrictedSuitInHandFallsBackToFullHand() {
            // North holds only hearts during opening phase — must still be allowed to play
            ContractState contract = hearts(5, 2)
                    .start(Player.NORTH, handsOf(
                            h("AH"), h("KS"), h("QS"), h("JS")));
            assertEquals(Set.of(c("AH")), contract.currentPlayerLegalCards());
        }
    }
}