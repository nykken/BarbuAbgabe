package org.engine.game;

import org.engine.card.Deck;
import org.engine.card.Hand;
import org.engine.card.Card;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Static utility for dealing cards.
 *
 * <p>The caller supplies a {@link Random} instance so it retains control of
 * the RNG sequence after dealing. Passing {@code new Random(seed)} reproduces
 * identical hands for the same seed; calling {@code rng.nextLong()} afterwards
 * advances the chain for the next deal.
 */
public class CardDealer {

    /**
     * Creates, shuffles, and deals a deck evenly to all players.
     *
     * @param variant the deck variant to deal from
     * @param rng     the random source used for shuffling; the caller may call
     *                {@code rng.nextLong()} afterwards to derive the next seed
     * @return dealt hands keyed by player
     * @throws IllegalArgumentException if the deck size is not divisible
     *                                  by the number of players
     */
    public static Map<Player, Hand> deal(Deck.Variant variant, Random rng) {
        Deck deck = variant.create();
        deck.shuffle(rng);

        Player[] players = Player.values();
        int cardsPerHand = deck.size() / players.length;

        if (deck.size() % players.length != 0) {
            throw new IllegalArgumentException(
                    "Deck of %d cards cannot be dealt evenly to %d players"
                            .formatted(deck.size(), players.length));
        }

        List<Card> cards = deck.getCards();
        Map<Player, Hand> hands = new EnumMap<>(Player.class);
        for (Player player : players) {
            int from = cardsPerHand * player.ordinal();
            int to = from + cardsPerHand;
            hands.put(player, new Hand(cards.subList(from, to)));
        }

        return Map.copyOf(hands);
    }

    private CardDealer() {}
}
