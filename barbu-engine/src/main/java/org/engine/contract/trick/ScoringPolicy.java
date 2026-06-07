package org.engine.contract.trick;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.game.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A distinct scoring element within a trick-taking contract.
 *
 * <p>Each element encapsulates one point-giving concept, likw a suit, a specific
 * card, a rank, or tricks themselves.
 *
 * <p>Lead restrictions are handled separately by {@link LeadRestriction}.
 * Composite scoring is supported via {@link CompositeScoringPolicy}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScoringPolicy.SuitScoresPoints.class, name = "SUIT"),
        @JsonSubTypes.Type(value = ScoringPolicy.CardScoresPoints.class, name = "CARD"),
        @JsonSubTypes.Type(value = ScoringPolicy.RankScoresPoints.class, name = "RANK"),
        @JsonSubTypes.Type(value = ScoringPolicy.TricksScorePoints.class, name = "TRICKS"),
        @JsonSubTypes.Type(value = ScoringPolicy.LastTwoTricksScorePoints.class, name = "LAST_TWO"),
        @JsonSubTypes.Type(value = ScoringPolicy.CompositeScoringPolicy.class, name = "COMPOSITE")
})
public interface ScoringPolicy {

    /**
     * Computes this element's score contribution from the played tricks.
     *
     * @param playedTricks all completed tricks in order
     * @return points per player for this element
     */
    Map<Player, Integer> score(List<Trick> playedTricks);

    /**
     * Returns {@code true} when this element can no longer contribute points.
     *
     * @param hands current hands of all players
     * @return {@code true} if this scoring element is exhausted
     */
    boolean isFinished(Map<Player, Hand> hands);

    // ── implementations ─────────────────────────────────────────────────────────

    /**
     * Each trick taken is worth {@code pointsPerTrick}.
     *
     * @param pointsPerTrick points awarded per trick taken
     */
    record TricksScorePoints(int pointsPerTrick) implements ScoringPolicy {
        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            Map<Player, Integer> scores = zeroScores();
            for (Trick trick : playedTricks) {
                scores.merge(trick.winner(), pointsPerTrick, Integer::sum);
            }
            return Collections.unmodifiableMap(scores);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return hands.values().stream().allMatch(Hand::isEmpty);
        }
    }


    /**
     * Awards points to the winner(s) of the final 2 tricks of the round.
     *
     * @param pointsSecondLast points for winning the second-to-last trick
     * @param pointsLast       points for winning the last trick
     */
    record LastTwoTricksScorePoints(int pointsSecondLast, int pointsLast) implements ScoringPolicy {
        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            if (playedTricks.size() < 2) {
                throw new IllegalStateException(
                        "LastTwoTricksScorePoints requires at least 2 played tricks, got "
                                + playedTricks.size());
            }
            Map<Player, Integer> scores = zeroScores();
            int size = playedTricks.size();

            Player secondLastWinner = playedTricks.get(size - 2).winner();
            scores.merge(secondLastWinner, pointsSecondLast, Integer::sum);

            Player lastWinner = playedTricks.get(size - 1).winner();
            scores.merge(lastWinner, pointsLast, Integer::sum);

            return Collections.unmodifiableMap(scores);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return hands.values().stream().allMatch(Hand::isEmpty);
        }

    }

    /**
     * Combines multiple scoring policies, summing their contributions per trick.
     *
     * @param children the policies to aggregate; must not be empty
     */
    record CompositeScoringPolicy(List<ScoringPolicy> children) implements ScoringPolicy {

        public CompositeScoringPolicy {
            children = List.copyOf(children);
        }

        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            Map<Player, Integer> totals = zeroScores();
            for (ScoringPolicy child : children) {
                child.score(playedTricks).forEach((p, pts) ->
                        totals.merge(p, pts, Integer::sum));
            }
            return Collections.unmodifiableMap(totals);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return children.stream().allMatch(c -> c.isFinished(hands));
        }
    }


    /**
     * A specific card is worth {@code points}.
     *
     * @param card   the point-scoring card
     * @param points points awarded to the player who takes this card
     */
    record CardScoresPoints(Card card, int points) implements ScoringPolicy {

        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            Map<Player, Integer> scores = zeroScores();
            for (Trick trick : playedTricks) {
                boolean taken = trick.cards().stream()
                        .anyMatch(c -> c.equals(card));
                if (taken) scores.merge(trick.winner(), points, Integer::sum);
            }
            return Collections.unmodifiableMap(scores);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return hands.values().stream()
                    .noneMatch(h -> h.contains(card));
        }
    }

    /**
     * Cards of a specific rank are worth {@code pointsPerCard} each.
     *
     * @param rank          the point-scoring rank
     * @param pointsPerCard points awarded per card of this rank taken
     */
    record RankScoresPoints(
            Rank rank,
            int pointsPerCard
    ) implements ScoringPolicy {

        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            Map<Player, Integer> scores = zeroScores();
            for (Trick trick : playedTricks) {
                long count = trick.cards().stream()
                        .filter(c -> c.rank() == rank)
                        .count();
                scores.merge(trick.winner(), (int) count * pointsPerCard, Integer::sum);
            }
            return Collections.unmodifiableMap(scores);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return hands.values().stream()
                    .noneMatch(h -> h.hasRank(rank));
        }
    }

    /**
     * Cards of a specific suit are worth {@code pointsPerCard} each.
     *
     * @param suit          the point-scoring suit
     * @param pointsPerCard points awarded per card of this suit taken
     */
    record SuitScoresPoints(Suit suit, int pointsPerCard) implements ScoringPolicy {

        @Override
        public Map<Player, Integer> score(List<Trick> playedTricks) {
            Map<Player, Integer> scores = zeroScores();
            for (Trick trick : playedTricks) {
                long count = trick.cards().stream()
                        .filter(c -> c.suit() == suit)
                        .count();
                scores.merge(trick.winner(), (int) count * pointsPerCard, Integer::sum);
            }
            return Collections.unmodifiableMap(scores);
        }

        @Override
        public boolean isFinished(Map<Player, Hand> hands) {
            return hands.values().stream()
                    .noneMatch(h -> h.hasSuit(suit));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Returns a mutable zero-initialised score map for all players. */
    static Map<Player, Integer> zeroScores() {
        Map<Player, Integer> scores = new EnumMap<>(Player.class);
        for (Player p : Player.values()) scores.put(p, 0);
        return scores;
    }
}