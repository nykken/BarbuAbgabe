package org.engine.helpers;

import org.engine.card.Card;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.contract.trick.LeadRestriction;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.contract.trick.ScoringPolicy;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Test-scope factory methods for building {@link org.engine.contract.Contract} instances.
 *
 * <p>Mirrors what {@code GameCatalog} provides in the application layer,
 * keeping engine tests self-contained. For building {@link org.engine.game.GameSettings},
 * use {@link TestSettings}.
 */
public final class TestContracts {
    private TestContracts() {}

    public static TrickTakingContract hearts(int pointsPerHeart, int openingPhaseLength) {
        return new TrickTakingContract(
                new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, pointsPerHeart),
                new LeadRestriction.OpeningPhase(Suit.HEARTS, openingPhaseLength));
    }

    public static TrickTakingContract hearts(int pointsPerHeart, LeadRestriction restriction) {
        return new TrickTakingContract(
                new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, pointsPerHeart),
                restriction);
    }

    public static TrickTakingContract tricks(int pointsPerTrick) {
        return new TrickTakingContract(
                new ScoringPolicy.TricksScorePoints(pointsPerTrick),
                LeadRestriction.NONE);
    }

    public static TrickTakingContract queens(int pointsPerQueen) {
        return new TrickTakingContract(
                new ScoringPolicy.RankScoresPoints(Rank.QUEEN, pointsPerQueen),
                LeadRestriction.NONE);
    }

    public static TrickTakingContract boys(int pointsPerBoy) {
        return new TrickTakingContract(
                new ScoringPolicy.CompositeScoringPolicy(List.of(
                        new ScoringPolicy.RankScoresPoints(Rank.JACK, pointsPerBoy),
                        new ScoringPolicy.RankScoresPoints(Rank.KING, pointsPerBoy))),
                LeadRestriction.NONE);
    }

    public static TrickTakingContract kingOfHearts(int points, int openingPhaseLength) {
        return new TrickTakingContract(
                new ScoringPolicy.CardScoresPoints(new Card(Suit.HEARTS, Rank.KING), points),
                new LeadRestriction.OpeningPhase(Suit.HEARTS, openingPhaseLength));
    }

    public static TrickTakingContract kingOfHearts(int points, LeadRestriction restriction) {
        return new TrickTakingContract(
                new ScoringPolicy.CardScoresPoints(new Card(Suit.HEARTS, Rank.KING), points),
                restriction);
    }

    public static TrickTakingContract lastTwo(int pointsSecondLast, int pointsLast) {
        return new TrickTakingContract(
                new ScoringPolicy.LastTwoTricksScorePoints(pointsSecondLast, pointsLast),
                LeadRestriction.NONE);
    }

    public static TrickTakingContract ratatouille(List<TrickTakingContract> parts) {
        return TrickTakingContract.combine(parts);
    }

    public static TrickTakingContract ratatouille(int pointsPerTrick, int pointsPerHeart,
                                                   int pointsPerQueen, int kingOfHeartsPoints,
                                                   int openingPhaseLength) {
        return new TrickTakingContract(
                new ScoringPolicy.CompositeScoringPolicy(List.of(
                        new ScoringPolicy.TricksScorePoints(pointsPerTrick),
                        new ScoringPolicy.SuitScoresPoints(Suit.HEARTS, pointsPerHeart),
                        new ScoringPolicy.RankScoresPoints(Rank.QUEEN, pointsPerQueen),
                        new ScoringPolicy.CardScoresPoints(
                                new Card(Suit.HEARTS, Rank.KING), kingOfHeartsPoints))),
                new LeadRestriction.OpeningPhase(Suit.HEARTS, openingPhaseLength));
    }

    public static ReussiteContract reussite(@Nullable Rank startingRank, List<Integer> placementPoints,
                                            Rank replayAfterRank) {
        return new ReussiteContract(startingRank, placementPoints, replayAfterRank);
    }


}
