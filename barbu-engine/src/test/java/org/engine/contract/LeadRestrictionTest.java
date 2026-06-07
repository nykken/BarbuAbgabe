package org.engine.contract;

import org.engine.card.Card;
import org.engine.card.Suit;
import org.engine.contract.trick.LeadRestriction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.engine.helpers.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadRestrictionTest {

    @Nested
    class NoneTest {

        @Test
        void allCardsAlwaysLegal() {
            Set<Card> hand = Set.of(c("AH"), c("AS"), c("AC"));
            assertEquals(hand, LeadRestriction.NONE.restrictLeads(hand, List.of()));
            assertEquals(hand, LeadRestriction.NONE.restrictLeads(hand, List.of(
                    t("AS", "KS", "QS", "JS"))));
        }
    }

    @Nested
    class OpeningPhaseTest {

        private final LeadRestriction restriction = new LeadRestriction.OpeningPhase(Suit.HEARTS, 2);

        @Test
        void heartsMayNotBeLeadBeforeOpeningPhase() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            Set<Card> legal = restriction.restrictLeads(hand, List.of());
            assertFalse(legal.contains(c("AH")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void heartsCanBeLeadAfterOpeningPhase() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            assertEquals(hand, restriction.restrictLeads(hand, List.of(
                    t("AS", "KS", "QS", "JS"),
                    t("AC", "KC", "QC", "JC"))));
        }

        @Test
        void restrictionLiftsExactlyAtOpeningPhaseBoundary() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            // exactly 2 tricks played = phase over
            assertEquals(hand, restriction.restrictLeads(hand, List.of(
                    t("AS", "2S", "3S", "4S"),
                    t("AC", "2C", "3C", "4C"))));
        }

        @Test
        void onlyHeartsInHandDuringOpeningPhase() {
            Set<Card> hand = Set.of(c("AH"), c("KH"));
            // fallback: all cards allowed when restriction would leave none
            assertEquals(hand, restriction.restrictLeads(hand, List.of()));
        }
    }

    @Nested
    class BrokenSuitTest {

        private final LeadRestriction restriction = new LeadRestriction.BrokenSuit(Suit.HEARTS);

        @Test
        void heartsMayNotBeLeadBeforeBroken() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            Set<Card> legal = restriction.restrictLeads(hand, List.of(
                    t("AS", "KS", "QS", "JS")));
            assertFalse(legal.contains(c("AH")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void heartsCanBeLeadOnceBroken() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            // East discards a heart in a spades trick — hearts are broken
            Set<Card> legal = restriction.restrictLeads(hand, List.of(
                    t("AS", "2H", "3S", "4S")));
            assertTrue(legal.contains(c("AH")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void heartLedDoesNotBreakHearts() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            // Hearts were led, so this does not count as "broken"
            Set<Card> legal = restriction.restrictLeads(hand, List.of(
                    t("AH", "2H", "3H", "4H")));
            assertFalse(legal.contains(c("AH")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void onlyHeartsInHandBeforeBroken() {
            Set<Card> hand = Set.of(c("AH"), c("KH"));
            // fallback: all cards allowed when restriction would leave none
            assertEquals(hand, restriction.restrictLeads(hand, List.of(
                    t("AS", "KS", "QS", "JS"))));
        }

        @Test
        void noTricksPlayedYet() {
            Set<Card> hand = Set.of(c("AH"), c("AS"));
            Set<Card> legal = restriction.restrictLeads(hand, List.of());
            assertFalse(legal.contains(c("AH")));
        }
    }

    @Nested
    class CompositeTest {

        @Test
        void bothRestrictionsApply() {
            LeadRestriction composite = new LeadRestriction.Composite(List.of(
                    new LeadRestriction.OpeningPhase(Suit.HEARTS, 2),
                    new LeadRestriction.OpeningPhase(Suit.CLUBS, 1)));
            Set<Card> hand = Set.of(c("AH"), c("AC"), c("AS"));

            // 0 tricks: both hearts and clubs restricted
            Set<Card> legal = composite.restrictLeads(hand, List.of());
            assertFalse(legal.contains(c("AH")));
            assertFalse(legal.contains(c("AC")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void firstRestrictionLiftedSecondStillApplies() {
            LeadRestriction composite = new LeadRestriction.Composite(List.of(
                    new LeadRestriction.OpeningPhase(Suit.HEARTS, 1),
                    new LeadRestriction.OpeningPhase(Suit.CLUBS, 3)));
            Set<Card> hand = Set.of(c("AH"), c("AC"), c("AS"));

            // 1 trick: hearts unrestricted, clubs still restricted
            Set<Card> legal = composite.restrictLeads(hand, List.of(
                    t("AS", "2S", "3S", "4S")));
            assertTrue(legal.contains(c("AH")));
            assertFalse(legal.contains(c("AC")));
            assertTrue(legal.contains(c("AS")));
        }

        @Test
        void fallbackWhenAllRestrictionsWouldLeaveNoCards() {
            LeadRestriction composite = new LeadRestriction.Composite(List.of(
                    new LeadRestriction.OpeningPhase(Suit.HEARTS, 5),
                    new LeadRestriction.OpeningPhase(Suit.SPADES, 5)));
            Set<Card> hand = Set.of(c("AH"), c("AS"));

            // After first restriction hearts removed → only AS left
            // After second restriction spades removed → empty → fallback to full hand
            assertEquals(hand, composite.restrictLeads(hand, List.of()));
        }
    }
}