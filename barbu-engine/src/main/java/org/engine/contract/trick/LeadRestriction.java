package org.engine.contract.trick;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.card.Suit;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines which cards the leading player may play at the start of a trick.
 *
 * <p>Implementations receive the full played-trick history so they can support
 * both count-based restrictions (opening phase) and event-based restrictions
 * (broken suit). When a restriction would leave no legal cards, the full hand
 * is returned as a fallback so a player is never stuck.
 *
 * <p>Compose multiple restrictions with {@link Composite}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LeadRestriction.None.class, name = "NONE"),
        @JsonSubTypes.Type(value = LeadRestriction.OpeningPhase.class, name = "OPENING_PHASE"),
        @JsonSubTypes.Type(value = LeadRestriction.BrokenSuit.class, name = "BROKEN_SUIT"),
        @JsonSubTypes.Type(value = LeadRestriction.Composite.class, name = "COMPOSITE")
})
public interface LeadRestriction {
    /**
     * Filters the cards the leading player may play.
     *
     * @param hand         the leading player's current hand
     * @param playedTricks all completed tricks in order
     * @return subset of {@code hand} that may be led. Equals {@code hand} when
     *         no restriction applies or when restriction would leave no legal cards.
     */
    Set<Card> restrictLeads(Set<Card> hand, List<Trick> playedTricks);


    /** No restriction */
    record None() implements LeadRestriction {
        @Override
        public Set<Card> restrictLeads(Set<Card> hand, List<Trick> playedTricks) {
            return hand;
        }
    }

    LeadRestriction NONE = new None();

    /**
     * A suit may not be led during the first {@code length} tricks.
     *
     * <p>Used for Hearts and King of Hearts contracts where hearts may not be
     * led until the opening phase ends, unless the player holds only hearts.
     */
    record OpeningPhase(Suit suit, int length) implements LeadRestriction {
        @Override
        public Set<Card> restrictLeads(Set<Card> hand, List<Trick> playedTricks) {
            if (playedTricks.size() >= length) {
                return hand;
            }
            Set<Card> filtered = hand.stream()
                    .filter(c -> c.suit() != suit)
                    .collect(Collectors.toUnmodifiableSet());
            return filtered.isEmpty() ? hand : filtered;
        }
    }

    /**
     * A suit may not be led until it has been played off-suit in a previous trick.
     *
     * <p>A suit is considered "broken" when any player has discarded a card of
     * that suit in a trick where it was not the led suit.
     */
    record BrokenSuit(Suit suit) implements LeadRestriction {
        @Override
        public Set<Card> restrictLeads(Set<Card> hand, List<Trick> playedTricks) {
            if (isBroken(playedTricks)) {
                return hand;
            }
            Set<Card> filtered = hand.stream()
                    .filter(c -> c.suit() != suit)
                    .collect(Collectors.toUnmodifiableSet());
            return filtered.isEmpty() ? hand : filtered;
        }

        private boolean isBroken(List<Trick> playedTricks) {
            return playedTricks.stream().anyMatch(trick ->
                    trick.cards().stream()
                            .anyMatch(c -> c.suit() == suit && trick.ledSuit() != suit));
        }
    }

    /**
     * Combines multiple restrictions.
     *
     * <p>If any step would produce an empty set, the full original hand is
     * returned (the player is never forced into an impossible position).
     */
    record Composite(List<LeadRestriction> restrictions) implements LeadRestriction {
        public Composite {
            restrictions = List.copyOf(restrictions);
        }

        @Override
        public Set<Card> restrictLeads(Set<Card> hand, List<Trick> playedTricks) {
            Set<Card> legal = hand;
            for (LeadRestriction restriction : restrictions) {
                Set<Card> allowed = restriction.restrictLeads(hand, playedTricks);
                Set<Card> intersection = legal.stream()
                        .filter(allowed::contains)
                        .collect(Collectors.toUnmodifiableSet());
                if (intersection.isEmpty()) return hand;
                legal = intersection;
            }
            return legal;
        }
    }
}