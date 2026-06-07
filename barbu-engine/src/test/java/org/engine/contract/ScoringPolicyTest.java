package org.engine.contract;

import org.engine.card.Card;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.trick.Trick;
import org.engine.contract.trick.ScoringPolicy;
import org.engine.game.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.engine.helpers.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoringPolicyTest {

    // -------------------------------------------------------------------------
    // SuitScoring
    // -------------------------------------------------------------------------

    @Nested
    class SuitScoresPointsTest {

        private final ScoringPolicy.SuitScoresPoints hearts =
                new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, 5);

        @Test
        void noHeartsTaken() {
            Map<Player, Integer> scores = hearts.score(List.of(
                    t("AS", "KS", "QS", "JS")));
            assertEquals(0, scores.get(Player.NORTH));
            assertEquals(0, scores.get(Player.EAST));
            assertEquals(0, scores.get(Player.SOUTH));
            assertEquals(0, scores.get(Player.WEST));
        }

        @Test
        void twoHeartsTakenByNorth() {
            Map<Player, Integer> scores = hearts.score(List.of(
                    t("AS", "2H", "3H", "2C")));
            assertEquals(10, scores.get(Player.NORTH));
            assertEquals(0,  scores.get(Player.EAST));
        }

        @Test
        void heartsSpreadAcrossTricks() {
            Map<Player, Integer> scores = hearts.score(List.of(
                    t("AS", "2H", "2C", "3C"),
                    t(Player.EAST, "2H", "AH", "3H", "4H")));
            assertEquals(5,  scores.get(Player.NORTH));
            assertEquals(20, scores.get(Player.EAST));
        }

        @Test
        void emptyTrickList() {
            assertEquals(0, hearts.score(List.of()).get(Player.NORTH));
        }

        @Test
        void heartsRemainingInHand() {
            assertFalse(hearts.isFinished(
                    handsOf(h("AH"), empty(), empty(), empty())));
        }

        @Test
        void noHeartsInAnyHand() {
            assertTrue(hearts.isFinished(
                    handsOf(h("AS"), empty(), empty(), empty())));
        }

        @Test
        void allHandsEmpty() {
            assertTrue(hearts.isFinished(
                    handsOf(empty(), empty(), empty(), empty())));
        }
    }

    // -------------------------------------------------------------------------
    // CardScoring
    // -------------------------------------------------------------------------

    @Nested
    class CardsScorePointsTest {

        private static final Card KOH = c("KH");
        private final ScoringPolicy.CardScoresPoints koh =
                new ScoringPolicy.CardScoresPoints(KOH, 40);

        @Test
        void kohNotInAnyTrick() {
            assertEquals(0, koh.score(List.of(
                    t("AS", "2S", "3S", "4S"))).get(Player.NORTH));
        }

        @Test
        void northTakesKoh() {
            Map<Player, Integer> scores = koh.score(List.of(
                    t("AS", "KH", "2S", "3S")));
            assertEquals(40, scores.get(Player.NORTH));
            assertEquals(0,  scores.get(Player.EAST));
        }

        @Test
        void kohScoredOnlyOnce() {
            Map<Player, Integer> scores = koh.score(List.of(
                    t("AS", "KH", "2S", "3S"),
                    t("AC", "2C", "3C", "4C")));
            assertEquals(40, scores.get(Player.NORTH));
            assertEquals(0,  scores.get(Player.EAST));
        }

        @Test
        void kohInHand() {
            assertFalse(koh.isFinished(
                    handsOf(empty(), h("KH"), empty(), empty())));
        }

        @Test
        void kohNotInAnyHand() {
            assertTrue(koh.isFinished(
                    handsOf(h("AS"), empty(), empty(), empty())));
        }
    }

    // -------------------------------------------------------------------------
    // RankScoring
    // -------------------------------------------------------------------------

    @Nested
    class RankScoresPointsTest {

        private final ScoringPolicy.RankScoresPoints queens =
                new ScoringPolicy.RankScoresPoints(Rank.QUEEN, 10);

        @Test
        void noQueensTaken() {
            assertEquals(0, queens.score(List.of(
                    t("AS", "KS", "JS", "10S"))).get(Player.NORTH));
        }

        @Test
        void twoQueensTakenByNorth() {
            assertEquals(20, queens.score(List.of(
                    t("AS", "QH", "QC", "2S"))).get(Player.NORTH));
        }

        @Test
        void queensSpreadAcrossPlayers() {
            Map<Player, Integer> scores = queens.score(List.of(
                    t("AS", "QH", "2S", "3S"),
                    t(Player.EAST, "QC", "AC", "2C", "3C")));
            assertEquals(10, scores.get(Player.NORTH));
            assertEquals(10, scores.get(Player.EAST));
            assertEquals(0, scores.get(Player.SOUTH));
            assertEquals(0, scores.get(Player.WEST));
        }

        @Test
        void queenRemainingInHand() {
            assertFalse(queens.isFinished(
                    handsOf(empty(), empty(), h("QS"), empty())));
        }

        @Test
        void noQueensInAnyHand() {
            assertTrue(queens.isFinished(
                    handsOf(h("AS"), empty(), empty(), empty())));
        }
    }

    // -------------------------------------------------------------------------
    // TrickScoring
    // -------------------------------------------------------------------------

    @Nested
    class TricksScorePointsTest {

        private final ScoringPolicy.TricksScorePoints tricksScorePoints =
                new ScoringPolicy.TricksScorePoints(5);

        @Test
        void noTricks() {
            assertEquals(0, tricksScorePoints.score(List.of()).get(Player.NORTH));
        }

        @Test
        void northWinsOneTrick() {
            assertEquals(5, tricksScorePoints.score(List.of(
                    t("AS", "2S", "3S", "4S"))).get(Player.NORTH));
        }

        @Test
        void tricksSpreadAcrossPlayers() {
            Map<Player, Integer> scores = tricksScorePoints.score(List.of(
                    t("AS", "2S", "3S", "4S"),
                    t(Player.EAST, "2C", "AC", "3C", "4C"),
                    t(Player.EAST, "2H", "AH", "3H", "4H")));
            assertEquals(5,  scores.get(Player.NORTH));
            assertEquals(10, scores.get(Player.EAST));
            assertEquals(0,  scores.get(Player.SOUTH));
            assertEquals(0,  scores.get(Player.WEST));
        }

        @Test
        void someHandsNotEmpty() {
            assertFalse(tricksScorePoints.isFinished(
                    handsOf(h("AS"), empty(), empty(), empty())));
        }

        @Test
        void allHandsEmpty() {
            assertTrue(tricksScorePoints.isFinished(
                    handsOf(empty(), empty(), empty(), empty())));
        }
    }

    // -------------------------------------------------------------------------
    // CompositeScoringPolicy
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // LastTwoTricksScorePoints
    // -------------------------------------------------------------------------

    @Nested
    class LastTwoTricksScorePointsTest {

        private final ScoringPolicy.LastTwoTricksScorePoints lastTwo =
                new ScoringPolicy.LastTwoTricksScorePoints(20, 10);

        @Test
        void scoresGoToWinnersOfFinalTwoTricks() {
            Map<Player, Integer> scores = lastTwo.score(List.of(
                    t("AS", "2S", "3S", "4S"),                     // North leads & wins (irrelevant)
                    t(Player.EAST, "2H", "AH", "3H", "4H"),        // East leads with AH → East wins
                    t(Player.SOUTH, "2C", "3C", "AC", "4C")));     // South leads with AC → South wins
            assertEquals(0,  scores.get(Player.NORTH));
            assertEquals(20, scores.get(Player.EAST));
            assertEquals(10, scores.get(Player.SOUTH));
            assertEquals(0,  scores.get(Player.WEST));
        }

        @Test
        void sameWinnerOfBothFinalTricksGetsBoth() {
            Map<Player, Integer> scores = lastTwo.score(List.of(
                    t("AS", "2S", "3S", "4S"),
                    t("AH", "2H", "3H", "4H")));
            assertEquals(30, scores.get(Player.NORTH));
            assertEquals(0,  scores.get(Player.EAST));
        }

        @Test
        void throwsOnEmptyTrickList() {
            assertThrows(IllegalStateException.class, () -> lastTwo.score(List.of()));
        }

        @Test
        void throwsOnSingleTrick() {
            assertThrows(IllegalStateException.class,
                    () -> lastTwo.score(List.of(t("AS", "2S", "3S", "4S"))));
        }

        @Test
        void notFinishedWhileAnyHandHasCards() {
            assertFalse(lastTwo.isFinished(handsOf(h("AS"), empty(), empty(), empty())));
        }

        @Test
        void finishedWhenAllHandsEmpty() {
            assertTrue(lastTwo.isFinished(handsOf(empty(), empty(), empty(), empty())));
        }
    }

    @Nested
    class CompositeScoringPolicyTest {

        // Ratatouille-style composite: tricks (5pts) + hearts (2pts each)
        private final ScoringPolicy composite =
                new ScoringPolicy.CompositeScoringPolicy(List.of(
                        new ScoringPolicy.TricksScorePoints(5),
                        new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, 2)));

        @Test
        void emptyTrickList() {
            Map<Player, Integer> scores = composite.score(List.of());
            assertEquals(0, scores.get(Player.NORTH));
            assertEquals(0, scores.get(Player.EAST));
            assertEquals(0, scores.get(Player.SOUTH));
            assertEquals(0, scores.get(Player.WEST));
        }

        @Test
        void scoresFromBothChildrenAccumulate() {
            // North wins trick containing 2 hearts: 5 (trick) + 4 (2 hearts × 2) = 9
            Map<Player, Integer> scores = composite.score(List.of(
                    t("AS", "2H", "3H", "2C")));
            assertEquals(9, scores.get(Player.NORTH));
            assertEquals(0, scores.get(Player.EAST));
        }

        @Test
        void scoresSpreadAcrossPlayers() {
            // North wins trick with no hearts: 5pts
            // East wins trick with 1 heart: 5 + 2 = 7pts
            Map<Player, Integer> scores = composite.score(List.of(
                    t("AS", "2S", "3S", "4S"),
                    t(Player.EAST, "2H", "AC", "3C", "4C")));
            assertEquals(5, scores.get(Player.NORTH));
            assertEquals(7, scores.get(Player.EAST));
            assertEquals(0, scores.get(Player.SOUTH));
            assertEquals(0, scores.get(Player.WEST));
        }

        @Test
        void isFinishedWhenAllChildrenFinished() {
            assertTrue(composite.isFinished(
                    handsOf(empty(), empty(), empty(), empty())));
        }

        @Test
        void notFinishedIfAnyChildUnfinished() {
            assertFalse(composite.isFinished(
                    handsOf(h("AS"), empty(), empty(), empty())));
        }

        @Test
        void notFinishedIfHeartRemainsEvenWithEmptyOtherHands() {
            assertFalse(composite.isFinished(
                    handsOf(empty(), h("2H"), empty(), empty())));
        }

        @Test
        void singleChildCompositeEquivalentToDirectPolicy() {
            ScoringPolicy wrapped = new ScoringPolicy.CompositeScoringPolicy(
                    List.of(new ScoringPolicy.TricksScorePoints(10)));
            ScoringPolicy direct = new ScoringPolicy.TricksScorePoints(10);

            List<Trick> tricks = List.of(t("AS", "2S", "3S", "4S"));
            assertEquals(direct.score(tricks), wrapped.score(tricks));
        }
    }
}