package org.engine.game;

import org.engine.card.Card;
import org.engine.card.Deck;
import org.engine.card.Hand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CardDealerTest {

    private static final int PLAYER_COUNT = Player.values().length;
    private static final long SEED = 42L;

    @ParameterizedTest
    @EnumSource(Deck.Variant.class)
    void dealsHandToEveryPlayer(Deck.Variant variant) {
        Map<Player, Hand> hands = CardDealer.deal(variant, new Random(SEED));

        for (Player player : Player.values()) {
            assertTrue(hands.containsKey(player),
                "Expected a hand for player " + player + " but none was dealt.");
        }
    }

    @ParameterizedTest
    @EnumSource(Deck.Variant.class)
    void dealsNoNullHands(Deck.Variant variant) {
        Map<Player, Hand> hands = CardDealer.deal(variant, new Random(SEED));

        for (Player player : Player.values()) {
            assertNotNull(hands.get(player),
                    "Hand for player " + player + " must not be null");
        }
    }

    @ParameterizedTest
    @EnumSource(Deck.Variant.class)
    void eachHandHasExpectedCardCount(Deck.Variant variant) {
        int expectedPerHand = variant.create().size() / PLAYER_COUNT;

        Map<Player, Hand> hands = CardDealer.deal(variant, new Random(SEED));

        for (Player player : Player.values()) {
            int actual = hands.get(player).size();
            assertEquals(expectedPerHand, actual,
                    "Player " + player + ": expected " + expectedPerHand +
                            " cards but got " + actual);
        }
    }

    @ParameterizedTest
    @EnumSource(Deck.Variant.class)
    void totalDealtCardCountIsCorrect(Deck.Variant variant) {
        int expectedTotalDealt = (variant.create().size() / PLAYER_COUNT) * PLAYER_COUNT;

        int actualTotalDealt = CardDealer.deal(variant, new Random(SEED)).values().stream()
                .mapToInt(Hand::size)
                .sum();

        assertEquals(expectedTotalDealt, actualTotalDealt,
                "Expected " + expectedTotalDealt + " total dealt cards but got " + actualTotalDealt);
    }

    @ParameterizedTest
    @EnumSource(Deck.Variant.class)
    void dealtCardsContainNoDuplicates(Deck.Variant variant) {
        List<Card> allDealt = CardDealer.deal(variant, new Random(SEED)).values().stream()
                .flatMap(hand -> hand.cards().stream())
                .toList();

        assertEquals(allDealt.size(), new HashSet<>(allDealt).size(),
                "Duplicate cards found in dealt hands");
    }

    // --- Determinism and RNG tests ---

    @Test
    void sameSeedProducesDeterministicDeals() {
        var hands1 = CardDealer.deal(Deck.Variant.STANDARD, new Random(99L));
        var hands2 = CardDealer.deal(Deck.Variant.STANDARD, new Random(99L));

        for (Player player : Player.values()) {
            assertEquals(hands1.get(player).cards(),
                         hands2.get(player).cards(),
                    "Player " + player + " received different cards with the same seed");
        }
    }

    @Test
    void differentSeedsProduceDifferentDeals() {
        var hands1 = CardDealer.deal(Deck.Variant.STANDARD, new Random(1L));
        var hands2 = CardDealer.deal(Deck.Variant.STANDARD, new Random(Long.MAX_VALUE));

        boolean allMatch = Arrays.stream(Player.values())
                .allMatch(p -> hands1.get(p).cards().equals(hands2.get(p).cards()));

        assertFalse(allMatch,
                "Different seeds produced identical hands for all players. Shuffle may be broken.");
    }

}