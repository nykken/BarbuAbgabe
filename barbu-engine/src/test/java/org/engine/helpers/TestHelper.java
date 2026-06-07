package org.engine.helpers;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.game.Player;
import org.engine.contract.trick.Trick;

import java.util.*;

public class TestHelper {
    /**
     * Parses a shorthand string into a Card.
     * Format: [Rank][Suit] e.g., "7H", "10S", "AD", "JC"
     */
    public static Card c(String notation) {
        if (notation == null || notation.length() < 2) {
            throw new IllegalArgumentException("Invalid card notation: " + notation);
        }

        String suitChar = notation.substring(notation.length() - 1).toUpperCase();
        String rankPart = notation.substring(0, notation.length() - 1).toUpperCase();

        Suit suit = switch (suitChar) {
            case "H" -> Suit.HEARTS;
            case "S" -> Suit.SPADES;
            case "D" -> Suit.DIAMONDS;
            case "C" -> Suit.CLUBS;
            default -> throw new IllegalArgumentException("Unknown suit: " + suitChar);
        };

        Rank rank = switch (rankPart) {
            case "2" -> Rank.TWO;
            case "3" -> Rank.THREE;
            case "4" -> Rank.FOUR;
            case "5" -> Rank.FIVE;
            case "6" -> Rank.SIX;
            case "7" -> Rank.SEVEN;
            case "8" -> Rank.EIGHT;
            case "9" -> Rank.NINE;
            case "10" -> Rank.TEN;
            case "J" -> Rank.JACK;
            case "Q" -> Rank.QUEEN;
            case "K" -> Rank.KING;
            case "A" -> Rank.ACE;
            default -> throw new IllegalArgumentException("Unknown rank: " + rankPart);
        };

        return new Card(suit, rank);
    }

    /**
     * Creates a Hand from shorthand notations.
     */
    public static Hand h(String... notations) {
        List<Card> cards = Arrays.stream(notations)
                .map(TestHelper::c)
                .toList();
        return new Hand(cards);
    }

    /**
     * Quickly builds the full map of dealtHands for a test scenario.
     */
    public static Map<Player, Hand> handsOf(Hand n, Hand e, Hand s, Hand w) {
        Map<Player, Hand> hands = new EnumMap<>(Player.class);
        hands.put(Player.NORTH, n);
        hands.put(Player.EAST, e);
        hands.put(Player.SOUTH, s);
        hands.put(Player.WEST, w);
        return hands;
    }

    /**
     * Helper for tests that don't care about certain players' dealtHands.
     */
    public static Hand empty() {
        return new Hand(Collections.emptyList());
    }


    public static Trick t(Player leader, String northCard, String eastCard,
                          String southCard, String westCard) {
        Map<Player, String> cardByPlayer = Map.of(
                Player.NORTH, northCard,
                Player.EAST,  eastCard,
                Player.SOUTH, southCard,
                Player.WEST,  westCard);

        Trick trick = new Trick();
        Player player = leader;
        for (int i = 0; i < Player.values().length; i++) {
            trick = trick.with(player, c(cardByPlayer.get(player)));
            player = player.next();
        }
        return trick;
    }

    /**
     * Builds a complete trick where NORTH leads.
     */
    public static Trick t(String northCard, String eastCard,
                          String southCard, String westCard) {
        return t(Player.NORTH, northCard, eastCard, southCard, westCard);
    }
}