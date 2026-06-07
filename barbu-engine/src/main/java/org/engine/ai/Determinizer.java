package org.engine.ai;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.ContractInProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Guesses a complete deal for the hidden cards, starting from the partial view a single
 * player actually has.
 */
public final class Determinizer {

    private static final int MAX_ATTEMPTS = 25;

    private Determinizer() {}

    /**
     * Samples opponent hands that obey known "this player cannot hold this card" constraints,
     * using rejection sampling.
     *
     * <p>It works in two steps:
     * <ol>
     *   <li>Lock in any card that has only one possible holder.</li>
     *   <li>Randomly deal the remaining cards and accept the first deal that breaks no constraint.</li>
     * </ol>
     *
     * <p>If no valid deal appears within {@link #MAX_ATTEMPTS} tries, the last random deal is
     * returned anyway.
     *
     * @param state the current game state, used to read hands and void constraints
     * @param pov   the player whose visible hand is kept fixed
     * @param rng   random source for shuffling
     * @throws IllegalStateException if the void constraints make it impossible to assign a card
     *                               to any opponent (indicates corrupted or inconsistent game state)
     */
    public static Map<Player, Hand> sampleHands(ActiveGameState state,
                                                         Player pov,
                                                         Random rng) {

        Map<Player, Hand> actual = state.hands();
        RedealContext context = buildRedealContext(actual, pov);

        Map<Player, Set<Card>> impossible = (state instanceof ContractInProgress cip)
                ? cip.activeContract().impossibleCards()
                : Map.of();

        Map<Player, List<Card>> fixedAssignments = new EnumMap<>(Player.class);
        for (Player opponent : context.opponents()) {
            fixedAssignments.put(opponent, new ArrayList<>());
        }
        Map<Player, Integer> remainingCapacity = new EnumMap<>(context.targetHandSizes());
        List<Card> unassignedCards = new ArrayList<>(context.unknownCards());

        // Deal cards that can only possibly be held by one player first
        applyFixedAssignments(unassignedCards, fixedAssignments, remainingCapacity,
                impossible, context.opponents());

        // Deal the remaining cards randomly, check if it is a valid deal according to the constraints
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Map<Player, Hand> proposedDeal = proposeDeal(actual, unassignedCards, fixedAssignments,
                    remainingCapacity, pov, context.opponents(), rng);
            if (respectsConstraints(proposedDeal, impossible, context.opponents())) {
                return proposedDeal;
            }
        }
        // fallback: return a deal regardless if it breaks a constraint
        return proposeDeal(actual, unassignedCards, fixedAssignments, remainingCapacity, pov, context.opponents(), rng);
    }


    private record RedealContext(
            List<Card> unknownCards,
            List<Player> opponents,
            Map<Player, Integer> targetHandSizes
    ) {}

    private static RedealContext buildRedealContext(Map<Player, Hand> actualHands, Player pov) {
        List<Card> unknownCards = new ArrayList<>();
        List<Player> opponents = new ArrayList<>();
        Map<Player, Integer> targetHandSizes = new EnumMap<>(Player.class);

        for (Player p : Player.values()) {
            if (p != pov) {
                unknownCards.addAll(actualHands.get(p).cards());
                opponents.add(p);
                targetHandSizes.put(p, actualHands.get(p).size());
            }
        }
        return new RedealContext(List.copyOf(unknownCards), List.copyOf(opponents), Map.copyOf(targetHandSizes));
    }


    private static Map<Player, Hand> proposeDeal(
            Map<Player, Hand> actualHands,
            List<Card> unassignedCards,
            Map<Player, List<Card>> fixedAssignments,
            Map<Player, Integer> remainingCapacity,
            Player pov,
            List<Player> opponents,
            Random rng) {

        Collections.shuffle(unassignedCards, rng);
        Map<Player, List<Card>> dealtCards = new EnumMap<>(Player.class);

        // Deal fixed assignments (cards that can only go to one player) first
        for (Player opponent : opponents) {
            dealtCards.put(opponent, new ArrayList<>(fixedAssignments.get(opponent)));
        }

        Iterator<Card> pile = unassignedCards.iterator();
        for (Player opponent : opponents) {
            List<Card> hand = dealtCards.get(opponent);
            int slotsToFill = remainingCapacity.get(opponent);
            for (int dealt = 0; dealt < slotsToFill; dealt++) {
                hand.add(pile.next());
            }
        }
        return buildHands(actualHands, dealtCards, pov, opponents);
    }

    private static boolean respectsConstraints(Map<Player, Hand> deal,
                                               Map<Player, Set<Card>> impossible,
                                               List<Player> opponents) {

        for (Player opponent : opponents) {
            Set<Card> forbidden = impossible.getOrDefault(opponent, Collections.emptySet());
            for (Card card : deal.get(opponent).cards()) {
                if (forbidden.contains(card)) return false;
            }
        }
        return true;
    }


    private static Map<Player, Hand> buildHands(
            Map<Player, Hand> actualHands,
            Map<Player, List<Card>> assignments,
            Player pov,
            List<Player> opponents) {

        Map<Player, Hand> result = new EnumMap<>(Player.class);
        result.put(pov, actualHands.get(pov));
        for (Player opponent : opponents) {
            result.put(opponent, new Hand(assignments.get(opponent)));
        }
        return Map.copyOf(result);
    }

    private static void applyFixedAssignments(
            List<Card> unassigned,
            Map<Player, List<Card>> assignments,
            Map<Player, Integer> remainingCapacity,
            Map<Player, Set<Card>> impossible,
            List<Player> opponents) {

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Card card : new ArrayList<>(unassigned)) {
                List<Player> eligible = new ArrayList<>();
                for (Player opponent : opponents) {
                    if (remainingCapacity.get(opponent) > 0
                            && !impossible.getOrDefault(opponent, Collections.emptySet()).contains(card)) {
                        eligible.add(opponent);
                    }
                }
                if (eligible.isEmpty()) {
                    throw new IllegalStateException("Card " + card + " cannot be assigned to any player");
                }
                if (eligible.size() == 1) {
                    Player assignee = eligible.getFirst();
                    assignments.get(assignee).add(card);
                    remainingCapacity.merge(assignee, -1, Integer::sum);
                    unassigned.remove(card);
                    changed = true;
                    break;
                }
            }
        }
    }
}