package com.barbu.catalog;

import org.engine.card.Card;
import org.engine.card.Deck;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.contract.trick.LeadRestriction;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.contract.trick.ScoringPolicy;
import org.engine.game.GameSettings;

import java.util.List;
import java.util.Optional;


/**
 * Static registry of all implemented game variants and their contracts.
 */
public final class GameCatalog {
    private GameCatalog() {}

    // ── contracts ─────────────────────────────────────────────────────────

    public static ContractDefinition hearts(String prefix, int points, LeadRestriction restriction) {
        return new ContractDefinition(prefix + "_hearts", "Hearts",
                new TrickTakingContract(new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, points), restriction));
    }

    public static ContractDefinition tricks(String prefix, int points) {
        return new ContractDefinition(prefix + "_tricks", "Tricks",
                new TrickTakingContract(new ScoringPolicy.TricksScorePoints(points), LeadRestriction.NONE));
    }

    public static ContractDefinition queens(String prefix, int points) {
        return new ContractDefinition(prefix + "_queens", "Queens",
                new TrickTakingContract(new ScoringPolicy.RankScoresPoints(Rank.QUEEN, points), LeadRestriction.NONE));
    }

    public static ContractDefinition kingOfHearts(String prefix, int points, LeadRestriction restriction) {
        return new ContractDefinition(prefix + "_king_of_hearts", "King of Hearts",
                new TrickTakingContract(new ScoringPolicy.CardScoresPoints(new Card(Suit.HEARTS, Rank.KING), points), restriction));
    }

    public static ContractDefinition ratatouille(String prefix, List<ContractDefinition> parts) {
        List<TrickTakingContract> trickContracts = parts.stream()
                .map(part -> (TrickTakingContract) part.contract())
                .toList();

        return new ContractDefinition(prefix + "_ratatouille", "Ratatouille",
                TrickTakingContract.combine(trickContracts));
    }

    public static ContractDefinition boys(String prefix, int points) {
        return new ContractDefinition(prefix + "_boys", "Boys",
                new TrickTakingContract(
                        new ScoringPolicy.CompositeScoringPolicy(List.of(
                                new ScoringPolicy.RankScoresPoints(Rank.KING, points),
                                new ScoringPolicy.RankScoresPoints(Rank.JACK, points))),
                        LeadRestriction.NONE));
    }

    public static ContractDefinition reussiteFromRank(String prefix, Rank startingRank, List<Integer> points, Rank replayAfter) {
        return new ContractDefinition(prefix + "_reussite", "Réussite",
                new ReussiteContract(startingRank, points, replayAfter));
    }

    public static ContractDefinition dynamicReussite(String prefix, List<Integer> points, Rank replayAfter) {
        return new ContractDefinition(prefix + "_reussite", "Réussite",
                new ReussiteContract(null, points, replayAfter));
    }

    public static ContractDefinition lastTwo(String prefix, int pointsSecondLast, int pointsLast) {
        return new ContractDefinition(prefix + "_last_two", "Last Two",
                new TrickTakingContract(
                        new ScoringPolicy.LastTwoTricksScorePoints(pointsSecondLast, pointsLast),
                        LeadRestriction.NONE));
    }

    // ── game variants ─────────────────────────────────────────────────────────

    private static final String STANDARD_ID = "standard";

    public static final GameVariant STANDARD;

    static {
        var leadRestriction = new LeadRestriction.OpeningPhase(Suit.HEARTS, 2);

        var heartsDef = hearts(STANDARD_ID, 5, leadRestriction);
        var tricksDef = tricks(STANDARD_ID, 5);
        var queensDef = queens(STANDARD_ID, 10);
        var kohDef    = kingOfHearts(STANDARD_ID, 40, leadRestriction);

        STANDARD = new GameVariant(
                STANDARD_ID, "Standard Barbu",
                List.of(
                        heartsDef,
                        tricksDef,
                        queensDef,
                        kohDef,
                        ratatouille(STANDARD_ID, List.of(heartsDef, tricksDef, queensDef, kohDef)),
                        reussiteFromRank(STANDARD_ID, Rank.JACK, List.of(-100, -60), Rank.ACE)
                ),
                Deck.Variant.FROM_SEVEN,
                GameSettings.RankingOrder.LOWEST_SCORE_WINS
        );
    }

    private static final String POSITIVE_ID = "positive";

    public static final GameVariant POSITIVE;

    static {
        var leadRestriction = new LeadRestriction.OpeningPhase(Suit.HEARTS, 2);

        var heartsDef = hearts(POSITIVE_ID, 5, leadRestriction);
        var tricksDef = tricks(POSITIVE_ID, 5);
        var queensDef = queens(POSITIVE_ID, 10);
        var kohDef    = kingOfHearts(POSITIVE_ID, 40, leadRestriction);

        POSITIVE = new GameVariant(
                POSITIVE_ID, "Positive Barbu",
                List.of(
                        heartsDef,
                        tricksDef,
                        queensDef,
                        kohDef,
                        ratatouille(POSITIVE_ID, List.of(heartsDef, tricksDef, queensDef, kohDef)),
                        reussiteFromRank(POSITIVE_ID, Rank.JACK, List.of(100, 60), Rank.ACE)
                ),
                Deck.Variant.FROM_SEVEN,
                GameSettings.RankingOrder.HIGHEST_SCORE_WINS
        );
    }




    private static final String EXTENDED_ID = "extended";

    public static final GameVariant EXTENDED;

    static {
        var leadRestriction = new LeadRestriction.BrokenSuit(Suit.HEARTS);

        var heartsDef = hearts(EXTENDED_ID, 5, leadRestriction);
        var tricksDef = tricks(EXTENDED_ID, 5);
        var queensDef = queens(EXTENDED_ID, 10);
        var kohDef    = kingOfHearts(EXTENDED_ID, 40, leadRestriction);
        var boysDef    = boys(EXTENDED_ID, 5);
        var lastTwoDef = lastTwo(EXTENDED_ID, 20, 10);

        EXTENDED = new GameVariant(
                EXTENDED_ID, "Extended Barbu",
                List.of(
                        heartsDef,
                        tricksDef,
                        queensDef,
                        kohDef,
                        lastTwoDef,
                        boysDef,
                        ratatouille(EXTENDED_ID, List.of(heartsDef, tricksDef, queensDef, kohDef, lastTwoDef)),
                        dynamicReussite(EXTENDED_ID, List.of(-100, -60), Rank.ACE)
                ),
                Deck.Variant.STANDARD,
                GameSettings.RankingOrder.LOWEST_SCORE_WINS
        );
    }

    // ── queries ─────────────────────────────────────────────────────────

    public static List<GameVariant> all() {
        return List.of(STANDARD, POSITIVE, EXTENDED);
    }

    public static Optional<GameVariant> findById(String id) {
        return all().stream()
                .filter(v -> v.id().equals(id))
                .findFirst();
    }
}