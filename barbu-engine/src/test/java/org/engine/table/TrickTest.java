package org.engine.table;

import org.engine.contract.trick.Trick;
import org.engine.game.Player;
import org.engine.card.Suit;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.engine.helpers.TestHelper.c;
import static org.junit.jupiter.api.Assertions.*;

public class TrickTest {

    @Test
    void isEmptyOnCreation() {
        assertTrue(new Trick().isEmpty());
    }

    @Test
    void ledSuitIsFirstCardSuit() {
        Trick trick = new Trick().with(Player.NORTH, c("JH"));
        assertEquals(Suit.HEARTS, trick.ledSuit());
    }

    @Test
    void ledSuitThrowsWhenEmpty() {
        assertThrows(IllegalStateException.class, () -> new Trick().ledSuit());
    }

    @Test
    void cardPlayedByReturnsCorrectCard() {
        Trick trick = new Trick().with(Player.NORTH, c("JH"));
        assertEquals(Optional.of(c("JH")), trick.cardPlayedBy(Player.NORTH));
    }

    @Test
    void cardPlayedByReturnsEmptyForPlayerWhoHasNotPlayed() {
        Trick trick = new Trick().with(Player.NORTH, c("JH"));
        assertEquals(Optional.empty(), trick.cardPlayedBy(Player.EAST));
    }

    @Test
    void withThrowsIfPlayerAlreadyPlayed() {
        Trick trick = new Trick().with(Player.NORTH, c("JH"));
        assertThrows(IllegalArgumentException.class, () ->
                trick.with(Player.NORTH, c("AS"))
        );
    }

    @Test
    void isCompleteAfterAllPlayersPlay() {
        Trick trick = new Trick()
                .with(Player.NORTH, c("JH"))
                .with(Player.EAST,  c("QH"))
                .with(Player.SOUTH, c("KH"))
                .with(Player.WEST,  c("AH"));
        assertTrue(trick.isComplete());
    }

    @Test
    void winnerIsHighestCardOfLedSuit() {
        Trick trick = new Trick()
                .with(Player.NORTH, c("JH"))
                .with(Player.EAST,  c("KH"))
                .with(Player.SOUTH, c("AS")) // different suit, doesn't count
                .with(Player.WEST,  c("10H"));
        assertEquals(Player.EAST, trick.winner());
    }

    @Test
    void winnerThrowsWhenTrickNotComplete() {
        Trick trick = new Trick().with(Player.NORTH, c("JH"));
        assertThrows(IllegalStateException.class, trick::winner);
    }

    @Test
    void cardsOfSuitReturnsOnlyMatchingCards() {
        Trick trick = new Trick()
                .with(Player.NORTH, c("JH"))
                .with(Player.EAST,  c("AS"))
                .with(Player.SOUTH, c("QH"))
                .with(Player.WEST,  c("KC"));
        assertEquals(2, trick.cardsOfSuit(Suit.HEARTS).size());
    }

    @Test
    void withDoesNotMutateOriginal() {
        Trick original = new Trick();
        original.with(Player.NORTH, c("JH"));
        assertTrue(original.isEmpty());
    }
}