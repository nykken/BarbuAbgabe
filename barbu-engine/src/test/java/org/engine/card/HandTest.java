package org.engine.card;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HandTest {

    private Hand hand;
    private final Card heartAce  = new Card(Suit.HEARTS, Rank.ACE);
    private final Card heartKing = new Card(Suit.HEARTS, Rank.KING);
    private final Card spadeAce  = new Card(Suit.SPADES, Rank.ACE);

    @BeforeEach
    void setUp() {
        hand = new Hand(List.of(heartAce, heartKing, spadeAce));
    }

    @Test
    void hasSuitReturnsTrueWhenPresent() {
        assertTrue(hand.hasSuit(Suit.HEARTS));
    }

    @Test
    void hasSuitReturnsFalseWhenAbsent() {
        assertFalse(hand.hasSuit(Suit.CLUBS));
    }

    @Test
    void cards() {
        assertEquals(Set.of(heartAce, heartKing), hand.getCardsOfSuit(Suit.HEARTS));
    }

    @Test
    void cardsOfSuitReturnsEmptyWhenAbsent() {
        assertTrue(hand.getCardsOfSuit(Suit.DIAMONDS).isEmpty());
    }

    @Test
    void withoutCardReducesSize() {
        Hand result = hand.withoutCard(heartAce);
        assertEquals(2, result.size());
        assertFalse(result.contains(heartAce));
    }

    @Test
    void withoutCardDoesNotMutateOriginal() {
        hand.withoutCard(heartAce);
        assertEquals(3, hand.size());
        assertTrue(hand.contains(heartAce));
    }

    @Test
    void withoutCardThrowsWhenCardNotInHand() {
        assertThrows(IllegalArgumentException.class, () ->
                hand.withoutCard(new Card(Suit.CLUBS, Rank.SEVEN))
        );
    }

    @Test
    void isEmptyAfterRemovingAllCards() {
        Hand result = hand.withoutCard(heartAce)
                         .withoutCard(heartKing)
                         .withoutCard(spadeAce);
        assertTrue(result.isEmpty());
    }
}