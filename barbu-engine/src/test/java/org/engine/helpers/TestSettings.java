package org.engine.helpers;

import org.engine.card.Deck;
import org.engine.card.Suit;
import org.engine.contract.Contract;
import org.engine.card.Rank;
import org.engine.contract.trick.LeadRestriction;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.game.GameSettings;

import java.util.Arrays;
import java.util.List;

import static org.engine.helpers.TestContracts.*;

/**
 * Test-scope factory methods for {@link GameSettings}.
 *
 * <p>Default assumptions (unless a method says otherwise):
 * <ul>
 *   <li>Deck: {@link Deck.Variant#FROM_SEVEN}</li>
 *   <li>Ranking: {@link GameSettings.RankingOrder#LOWEST_SCORE_WINS}</li>
 * </ul>
 *
 * <p>Use {@link #of} for quick ad-hoc settings in focused unit tests.
 * Use {@link #standard} for full end-to-end scenarios.
 */
public final class TestSettings {
    private TestSettings() {}

    /**
     * Settings with the given contracts, FROM_SEVEN deck, LOWEST_SCORE_WINS.
     * Shorthand for the boilerplate in most unit tests.
     */
    public static GameSettings of(Contract... contracts) {
        return new GameSettings(
                Arrays.asList(contracts),
                Deck.Variant.FROM_SEVEN,
                GameSettings.RankingOrder.LOWEST_SCORE_WINS);
    }

    /**
     * Settings with the given contracts, FROM_SEVEN deck, HIGHEST_SCORE_WINS.
     */
    public static GameSettings highestScoreWins(Contract... contracts) {
        return new GameSettings(
                Arrays.asList(contracts),
                Deck.Variant.FROM_SEVEN,
                GameSettings.RankingOrder.HIGHEST_SCORE_WINS);
    }

    /**
     * Full standard Barbu game: six contracts, FROM_SEVEN deck, LOWEST_SCORE_WINS.
     * Equivalent to the former {@code GameSettings.standard()}.
     */
    public static GameSettings standard() {
        return new GameSettings(
                List.of(
                        hearts(5, 2),
                        tricks(5),
                        queens(10),
                        kingOfHearts(40, 2),
                        ratatouille(5, 5, 10, 40, 2),
                        reussite(Rank.JACK, List.of(-100, -60), Rank.ACE)
                ),
                Deck.Variant.FROM_SEVEN,
                GameSettings.RankingOrder.LOWEST_SCORE_WINS);
    }

    /**
     * Full extended Barbu game: eight contracts, STANDARD deck, LOWEST_SCORE_WINS.
     * Equivalent to GameCatalog.EXTENDED.
     */
    public static GameSettings extended() {
        var brokenSuit = new LeadRestriction.BrokenSuit(Suit.HEARTS);
        TrickTakingContract h   = hearts(5, brokenSuit);
        TrickTakingContract t   = tricks(5);
        TrickTakingContract q   = queens(10);
        TrickTakingContract koh = kingOfHearts(40, brokenSuit);
        TrickTakingContract lt  = lastTwo(20, 10);
        return new GameSettings(
                List.of(
                        h, t, q, koh, lt,
                        boys(5),
                        ratatouille(List.of(h, t, q, koh, lt)),
                        reussite(null, List.of(-100, -60), Rank.ACE)
                ),
                Deck.Variant.STANDARD,
                GameSettings.RankingOrder.LOWEST_SCORE_WINS);
    }
}
