package org.engine.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    static Stream<Deck> decks() {
        return Stream.of(Deck.Variant.FROM_SEVEN.create(), Deck.Variant.STANDARD.create());
    }

    @Test
    void fromSevenHas32Cards() {
        assertEquals(32, Deck.Variant.FROM_SEVEN.create().size());
    }

    @Test
    void standardHas52Cards() {
        assertEquals(52, Deck.Variant.STANDARD.create().size());
    }


    @ParameterizedTest
    @MethodSource("decks")
    void shufflePreservesAllCards(Deck deck) {
        Set<Card> before = Set.copyOf(deck.getCards());
        deck.shuffle(new Random(42));
        Set<Card> after = Set.copyOf(deck.getCards());
        assertEquals(before, after);
    }

    @Test
    void shuffleWithSameSeedProducesSameOrder() {
        Deck deck1 = Deck.Variant.FROM_SEVEN.create();
        Deck deck2 = Deck.Variant.FROM_SEVEN.create();
        deck1.shuffle(new Random(42));
        deck2.shuffle(new Random(42));
        assertEquals(deck1.getCards(), deck2.getCards());
    }

    @ParameterizedTest
    @MethodSource("decks")
    void shuffleChangesOrder(Deck deck) {
        List<Card> before = new ArrayList<>(deck.getCards());
        deck.shuffle(new Random(42));
        assertNotEquals(before, deck.getCards());
    }
}