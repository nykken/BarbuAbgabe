package org.engine.table;

import org.engine.card.Card;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.reussite.Tableau;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.engine.helpers.TestHelper.c;
import static org.junit.jupiter.api.Assertions.*;

public class TableauTest {

    @Nested
    class FixedStartingRank {

        @ParameterizedTest
        @EnumSource(Suit.class)
        void jackIsLegalOnEmptyPile(Suit suit) {
            assertTrue(new Tableau(Rank.JACK).isLegal(new Card(suit, Rank.JACK)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"AS", "10H", "KC"})
        void nonJackIsIllegalOnEmptyPile(String card) {
            assertFalse(new Tableau(Rank.JACK).isLegal(c(card)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"10H", "QH"})
        void immediateNeighbourIsLegalAfterStart(String card) {
            Tableau t = new Tableau(Rank.JACK).with(c("JH"));
            assertTrue(t.isLegal(c(card)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"9D", "KD"})
        void nonAdjacentCardIsIllegal(String card) {
            Tableau t = new Tableau(Rank.JACK).with(c("JD"));
            assertFalse(t.isLegal(c(card)));
        }

        @Test
        void sevenIsLegalAsBottomOfPile() {
            Tableau t = new Tableau(Rank.JACK)
                    .with(c("JH"))
                    .with(c("10H"))
                    .with(c("9H"))
                    .with(c("8H"));
            assertTrue(t.isLegal(c("7H")));
        }

        @Test
        void aceIsLegalAsTopOfPile() {
            Tableau t = new Tableau(Rank.JACK)
                    .with(c("JH"))
                    .with(c("QH"))
                    .with(c("KH"));
            assertTrue(t.isLegal(c("AH")));
        }

        @Test
        void withThrowsOnIllegalCard() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Tableau(Rank.JACK).with(c("10H"))
            );
        }

        @Test
        void getPilesIsUnmodifiable() {
            Tableau t = new Tableau(Rank.JACK).with(c("JH"));
            assertThrows(UnsupportedOperationException.class, () ->
                    t.getPiles().put(Suit.SPADES, new Tableau.SuitPile(Rank.JACK, Rank.JACK))
            );
        }

        @Test
        void withDoesNotMutateOriginal() {
            Tableau original = new Tableau(Rank.JACK);
            original.with(c("JH"));
            assertFalse(original.isLegal(c("10H")),
                    "Original should still be empty — jack not yet placed");
        }
    }

    @Nested
    class DynamicStartingRank {

        @Test
        void anyCardIsLegalOnCompletelyEmptyBoard() {
            Tableau t = new Tableau();
            assertTrue(t.isLegal(c("AS")));
            assertTrue(t.isLegal(c("10H")));
            assertTrue(t.isLegal(c("KC")));
        }

        @Test
        void firstCardSetsStartingRankForOtherSuits() {
            Tableau t = new Tableau().with(c("8H"));

            assertTrue(t.isLegal(c("8S")));
            assertTrue(t.isLegal(c("8D")));
            assertFalse(t.isLegal(c("7S")));
            assertFalse(t.isLegal(c("9C")));
        }

        @Test
        void adjacentCardsLegalAfterDynamicStart() {
            Tableau t = new Tableau().with(c("10C"));

            assertTrue(t.isLegal(c("9C")));
            assertTrue(t.isLegal(c("JC")));
            assertFalse(t.isLegal(c("8C")));
        }
    }

    @Nested
    class ArbitraryFixedStartingRanks {

        @ParameterizedTest
        @EnumSource(Rank.class)
        void anyRankCanBeSetAsTheStartingRank(Rank startingRank) {
            assertTrue(new Tableau(startingRank).isLegal(new Card(Suit.SPADES, startingRank)));
        }

        @Test
        void sevenStartsCorrectlyAndAllowsAdjacentCards() {
            Tableau t = new Tableau(Rank.SEVEN).with(c("7H"));

            assertTrue(t.isLegal(c("6H")));
            assertTrue(t.isLegal(c("8H")));
            assertFalse(t.isLegal(c("JH")));
        }
    }
}