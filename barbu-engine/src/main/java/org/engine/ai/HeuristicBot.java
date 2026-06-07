package org.engine.ai;

import org.engine.card.Card;
import org.engine.card.Deck;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.contract.reussite.ReussiteState;
import org.engine.contract.reussite.Tableau;
import org.engine.contract.trick.ScoringPolicy;
import org.engine.contract.trick.Trick;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.contract.trick.TrickTakingState;
import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.ContractInProgress;
import org.engine.game.state.WaitingForContractSelection;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A contract-aware heuristic bot. Faster than MCTS, smarter than random.
 *
 * <p>Scoring uses hand-tuned weights (inline constants in the {@code *Score}/{@code *Danger}
 * methods); they are empirical, not derived, and only their relative magnitudes matter.
 */
public class HeuristicBot extends Bot {

    @Override
    protected Move search(ActiveGameState state) {
        return switch (state) {
            case WaitingForContractSelection s -> selectContract(s);
            case ContractInProgress s -> playContract(s);
        };
    }

    // ── contract selection ────────────────────────────────────────────────────────

    private Move selectContract(WaitingForContractSelection state) {
        Set<Card> hand = state.hands().get(state.currentPlayer()).cards();
        Deck.Variant variant = state.settings().deckVariant();
        double deckAvgRank = (variant.minRank().getValue() + Rank.ACE.getValue()) / 2.0;
        int numRanks = Rank.ACE.getValue() - variant.minRank().getValue() + 1;
        return state.legalMoves().stream()
                .min(Comparator.comparingDouble(m -> contractDanger((Move.SelectContract) m, hand, deckAvgRank, numRanks)))
                .orElseThrow();
    }

    private double contractDanger(Move.SelectContract move,
                                  Set<Card> hand, double deckAvgRank, int numRanks) {

        if (move.contract() instanceof TrickTakingContract tc) {
            return policyDanger(tc.scoringPolicy(), hand, deckAvgRank);
        }
        return reussiteDanger((ReussiteContract) move.contract(), hand, numRanks);
    }

    private double policyDanger(ScoringPolicy policy, Set<Card> hand, double deckAvgRank) {
        switch (policy) {
            case ScoringPolicy.SuitScoresPoints(Suit suit, int pointsPerCard) -> {
                long count = hand.stream().filter(c -> c.suit() == suit).count();
                return (count - hand.size() / 4.0) * pointsPerCard * 0.4;
            }
            case ScoringPolicy.RankScoresPoints(Rank rank, int pointsPerCard) -> {
                long count = hand.stream().filter(c -> c.rank() == rank).count();
                return (count - 1.0) * pointsPerCard * 0.4;
            }
            case ScoringPolicy.CardScoresPoints(Card card, int points) -> {
                double scale = points / 10.0;
                if (!hand.contains(card)) return -1.25 * scale;
                int penaltyRank = card.rank().getValue();
                Suit suit = card.suit();
                long above = hand.stream().filter(x -> x.suit() == suit && x.rank().getValue() > penaltyRank).count();
                long below = hand.stream().filter(x -> x.suit() == suit && x.rank().getValue() < penaltyRank).count();
                return (3.75 + above * 0.5 - below * 0.3) * scale;
            }
            case ScoringPolicy.TricksScorePoints t -> {
                double sum = hand.stream().mapToInt(c -> c.rank().getValue()).sum();
                return (sum - hand.size() * deckAvgRank) * t.pointsPerTrick() / 50.0;
            }
            case ScoringPolicy.LastTwoTricksScorePoints l -> {
                double avg = hand.stream().mapToInt(c -> c.rank().getValue()).average().orElse(deckAvgRank);
                double avgPoints = (l.pointsSecondLast() + l.pointsLast()) / 2.0;
                return (avg - deckAvgRank) * avgPoints / 50.0;
            }
            case ScoringPolicy.CompositeScoringPolicy composite -> {
                return composite.children().stream().mapToDouble(child -> policyDanger(child, hand, deckAvgRank)).sum();
            }
            default -> {
            }
        }
        return 0.0;
    }

    // ── in-contract moves ────────────────────────────────────────────────────────

    private Move playContract(ContractInProgress state) {
        Player me = state.currentPlayer();
        int ranksInDeck = Rank.ACE.getValue() - state.settings().deckVariant().minRank().getValue() + 1;
        return switch (state.activeContract()) {
            case TrickTakingState ts -> playTrickTaking(me, ts, ranksInDeck);
            case ReussiteState rs    -> playReussite(me, rs);
            default                  -> state.legalMoves().getFirst();
        };
    }

    // ── trick-taking moves ────────────────────────────────────────────────────────

    private Move playTrickTaking(Player me, TrickTakingState state, int ranksInDeck) {
        List<Card> legalCards = legalPlayCards(me, state.legalMoves());
        ScoringPolicy policy = ((TrickTakingContract) state.contract()).scoringPolicy();
        Trick trick = state.playArea();
        Card chosen = trick.isEmpty()
                ? lead(legalCards, policy, state, ranksInDeck)
                : follow(legalCards, trick, policy, state);
        return new Move.PlayCard(me, chosen);
    }

    private Card lead(List<Card> legal, ScoringPolicy policy, TrickTakingState state, int ranksInDeck) {
        return legal.stream()
                .map(Card::suit)
                .distinct()
                .map(suit -> candidateLead(legal.stream().filter(c -> c.suit() == suit).toList(), policy, state, ranksInDeck))
                .max(Comparator.comparingDouble(c -> leadScore(c, legal, policy)))
                .orElseGet(() -> lowest(legal));
    }

    /**
     * Within a single suit, pick the card to lead.
     * Early in the suit and no opponent known void: lead the highest
     * non-penalty card. Likely to be beaten, unloading a dangerous high card for free.
     * Otherwise lead the lowest non-penalty card
     */
    private Card candidateLead(List<Card> suitCards, ScoringPolicy policy, TrickTakingState state, int ranksInDeck) {
        List<Card> nonPenalty = suitCards.stream().filter(c -> !isPenalty(c, policy)).toList();
        Card safeDefault = nonPenalty.isEmpty() ? lowest(suitCards) : lowest(nonPenalty);

        if (nonPenalty.isEmpty() || hasAvoidTricksComponent(policy)) return safeDefault;

        Suit suit = suitCards.getFirst().suit();
        long playedOfSuit = state.playedTricks().stream()
                .flatMap(t -> t.cards().stream())
                .filter(c -> c.suit() == suit)
                .count();

        if (ranksInDeck - playedOfSuit <= 6) return safeDefault;

        // Skip if any player is known void in this suit (they'd dump penalty cards on our trick)
        boolean anyVoid = state.impossibleCards().values().stream()
                .anyMatch(impossible -> impossible.stream().anyMatch(c -> c.suit() == suit));
        if (anyVoid) {
            return safeDefault;
        }

        return highest(nonPenalty);
    }

    /** Score a candidate lead card (higher = better), using only our own hand. */
    private double leadScore(Card card, List<Card> hand, ScoringPolicy policy) {
        double score = 0.0;
        // Penalise leading a card that scores against us; floor at 5.0, scales up for severe cards
        if (!isAvoidTricks(policy)) {
            int pp = cardPenaltyPoints(card, policy);
            if (pp > 0) score -= Math.max(5.0, 2.0 + pp * 0.5);
        }
        // Prefer shorter suits: exhausting a suit faster creates discard opportunities
        long suitCount = hand.stream().filter(c -> c.suit() == card.suit()).count();
        score -= suitCount * 0.5;
        // Prefer lower ranks: less likely to win the trick
        score -= card.rank().getValue() * 0.1;
        return score;
    }

    private Card follow(List<Card> legal, Trick trick, ScoringPolicy policy, TrickTakingState state) {
        Suit led = trick.ledSuit();
        boolean inSuit = legal.stream().anyMatch(c -> c.suit() == led);
        return inSuit ? followInSuit(legal, trick, led, policy, state) : discard(legal, policy);
    }

    private Card followInSuit(List<Card> legal, Trick trick, Suit led, ScoringPolicy policy, TrickTakingState state) {
        int winnerRank = trick.cardsOfSuit(led).stream()
                .mapToInt(c -> c.rank().getValue())
                .max()
                .orElse(0);

        List<Card> losing = legal.stream().filter(c -> c.rank().getValue() < winnerRank).toList();
        List<Card> winning = legal.stream().filter(c -> c.rank().getValue() >= winnerRank).toList();
        boolean isLast = trick.cards().size() == 3;

        if (losing.isEmpty()) {
            if (isLast || isAvoidTricks(policy)) return highestSafeWinner(winning, policy, state, led);
            int lowestWinningRank = lowest(winning).rank().getValue();
            if (canBeBeatenBy(lowestWinningRank, led, winning, trick, state)) return lowest(winning);
            return highestSafeWinner(winning, policy, state, led);
        }

        if (isAvoidTricks(policy)) return highest(losing);

        List<Card> penaltyLosing = losing.stream().filter(c -> isPenalty(c, policy)).toList();
        if (!penaltyLosing.isEmpty()) {
            return penaltyLosing.stream()
                    .max(Comparator.comparingInt((Card c) -> cardPenaltyPoints(c, policy))
                            .thenComparingInt(c -> c.rank().getValue()))
                    .orElseThrow();
        }
        return highest(losing);
    }


    private Card highestSafeWinner(List<Card> winning, ScoringPolicy policy, TrickTakingState state, Suit ledSuit) {
        if (policy instanceof ScoringPolicy.RankScoresPoints r) {
            boolean penaltyRankInHand = winning.stream().anyMatch(c -> c.rank() == r.rank());
            boolean penaltyRankSeen = state.playArea().cardsOfSuit(ledSuit).stream().anyMatch(c -> c.rank() == r.rank())
                    || state.playedTricks().stream().flatMap(t -> t.cards().stream())
                            .anyMatch(c -> c.suit() == ledSuit && c.rank() == r.rank());
            if (!penaltyRankInHand && !penaltyRankSeen) {
                return winning.stream()
                        .filter(c -> c.rank().getValue() < r.rank().getValue())
                        .max(byRank())
                        .orElseGet(() -> highest(winning));
            }
            return highest(winning);
        }
        if (policy instanceof ScoringPolicy.CompositeScoringPolicy) {
            // Among forced winners, prefer lowest card-level penalty; break ties by highest rank
            // (shedding a high card clears future trick-winning danger)
            return winning.stream()
                    .min(Comparator.comparingInt((Card c) -> cardPenaltyPoints(c, policy))
                            .thenComparingInt(c -> -c.rank().getValue()))
                    .orElseGet(() -> highest(winning));
        }
        Card high = highest(winning);
        if (isPenalty(high, policy)) {
            return winning.stream()
                    .filter(c -> !isPenalty(c, policy))
                    .max(byRank())
                    .orElse(high);
        }
        return high;
    }

    /**
     * Returns true if at least one player who hasn't yet played in this trick could plausibly
     * hold a card of {@code led} with rank strictly above {@code rank}.
     */
    private static boolean canBeBeatenBy(int rank, Suit led, List<Card> ourSuitCards,
                                          Trick trick, TrickTakingState state) {
        // How many cards of this suit with rank > ours could still exist?
        long possibleHigher = Rank.ACE.getValue() - rank;
        long accountedHigher =
                state.playedTricks().stream().flatMap(t -> t.cards().stream())
                        .filter(c -> c.suit() == led && c.rank().getValue() > rank).count()
                + trick.cards().stream()
                        .filter(c -> c.suit() == led && c.rank().getValue() > rank).count()
                + ourSuitCards.stream()
                        .filter(c -> c.rank().getValue() > rank).count();
        if (accountedHigher >= possibleHigher) return false; // all higher cards accounted for

        // At least one unaccounted higher card exists, is any remaining player not known void?
        Set<Player> alreadyPlayed = new HashSet<>(trick.playOrder());
        alreadyPlayed.add(state.currentPlayer());
        return Arrays.stream(Player.values())
                .filter(p -> !alreadyPlayed.contains(p))
                .anyMatch(p -> state.impossibleCards().get(p).stream().noneMatch(c -> c.suit() == led));
    }

    private Card discard(List<Card> legal, ScoringPolicy policy) {
        // Dump the highest-penalty card first (most points, rank as tiebreaker)
        Optional<Card> penaltyCard = legal.stream()
                .filter(c -> isPenalty(c, policy))
                .max(Comparator.comparingInt((Card c) -> cardPenaltyPoints(c, policy))
                        .thenComparingInt(c -> c.rank().getValue()));
        if (penaltyCard.isPresent()) return penaltyCard.get();

        // No penalty cards: void the shortest suit first to maximise future discard slots,
        // and within that suit dump the highest card to clear hand danger
        Suit shortestSuit = legal.stream()
                .map(Card::suit)
                .distinct()
                .min(Comparator.comparingLong(s -> legal.stream().filter(c -> c.suit() == s).count()))
                .orElseThrow();
        return legal.stream()
                .filter(c -> c.suit() == shortestSuit)
                .max(byRank())
                .orElseGet(() -> highest(legal));
    }

    // ── reussite moves ────────────────────────────────────────────────────────

    private double reussiteDanger(ReussiteContract rc, Set<Card> hand, int numRanks) {
        int proximity = 2; // within 2 rank steps is "near"
        long farCount;
        if (rc.startingRank() != null) {
            int anchor = rc.startingRank().getValue();
            farCount = hand.stream()
                    .filter(c -> Math.abs(c.rank().getValue() - anchor) > proximity)
                    .count();
        } else {
            // Dynamic rank: pick the anchor that minimises far cards
            long bestNearCount = hand.stream()
                    .map(c -> c.rank().getValue())
                    .distinct()
                    .mapToLong(anchor -> hand.stream()
                            .filter(c -> Math.abs(c.rank().getValue() - anchor) <= proximity)
                            .count())
                    .max()
                    .orElse(0L);
            farCount = hand.size() - bestNearCount;
        }
        // Centre around the expected far-count for a uniformly random hand
        double expectedFarCount = hand.size() * Math.max(0, numRanks - (2 * proximity + 1)) / (double) numRanks;
        return (farCount - expectedFarCount) * 1.2;
    }

    private Move playReussite(Player me, ReussiteState state) {
        List<Move> legal = state.legalMoves();
        if (legal.size() == 1 && legal.getFirst() instanceof Move.Pass) {
            return legal.getFirst();
        }
        List<Card> legalCards = legalPlayCards(me, legal);
        Map<Suit, Tableau.SuitPile> piles = state.playArea().getPiles();
        Set<Card> myHand = state.hands().get(me).cards();
        int minRank = minRankInGame(state);

        // Always prefer a card that immediately opens a slot we also hold (chain play)
        List<Card> chainCards = legalCards.stream()
                .filter(c -> opensSlotInHand(c, piles, myHand))
                .toList();
        List<Card> candidates = chainCards.isEmpty() ? legalCards : chainCards;

        Card chosen = candidates.stream()
                .max(Comparator.comparingDouble(c -> reussitePlayScore(c, piles, myHand, minRank)))
                .orElseThrow();
        return new Move.PlayCard(me, chosen);
    }

    private static int minRankInGame(ReussiteState state) {
        int fromHands = state.hands().values().stream()
                .flatMap(h -> h.cards().stream())
                .mapToInt(c -> c.rank().getValue())
                .min().orElse(Rank.ACE.getValue());
        int fromPiles = state.playArea().getPiles().values().stream()
                .mapToInt(p -> p.low().getValue())
                .min().orElse(Rank.ACE.getValue());
        return Math.min(fromHands, fromPiles);
    }

    private static double reussitePlayScore(Card c, Map<Suit, Tableau.SuitPile> piles,
                                            Set<Card> myHand, int minRank) {
        Suit suit = c.suit();
        int rankValue = c.rank().getValue();
        long otherInSuit = myHand.stream().filter(x -> x.suit() == suit && !x.equals(c)).count();
        double score = otherInSuit * 0.5;

        Tableau.SuitPile pile = piles.get(suit);
        if (pile == null) {
            score += openSafety(rankValue + 1, suit, myHand, minRank);
            score += openSafety(rankValue - 1, suit, myHand, minRank);
        } else if (rankValue == pile.high().getValue() + 1) {
            score += openSafety(rankValue + 1, suit, myHand, minRank);
        } else {
            score += openSafety(rankValue - 1, suit, myHand, minRank);
        }
        return score;
    }

    private static boolean opensSlotInHand(Card c, Map<Suit, Tableau.SuitPile> piles, Set<Card> myHand) {
        Suit suit = c.suit();
        int rankValue = c.rank().getValue();
        Tableau.SuitPile pile = piles.get(suit);
        if (pile == null) {
            return holdsRank(rankValue + 1, suit, myHand) || holdsRank(rankValue - 1, suit, myHand);
        } else if (rankValue == pile.high().getValue() + 1) {
            return holdsRank(rankValue + 1, suit, myHand);
        } else {
            return holdsRank(rankValue - 1, suit, myHand);
        }
    }

    private static boolean holdsRank(int rankVal, Suit suit, Set<Card> myHand) {
        return myHand.stream().anyMatch(x -> x.suit() == suit && x.rank().getValue() == rankVal);
    }

    private static double openSafety(int openedRankVal, Suit suit, Set<Card> myHand, int minRank) {
        if (openedRankVal > Rank.ACE.getValue() || openedRankVal < minRank) return 3.0;
        boolean holdsIt = myHand.stream()
                .anyMatch(x -> x.suit() == suit && x.rank().getValue() == openedRankVal);
        return holdsIt ? 2.0 : -2.0;
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static List<Card> legalPlayCards(Player me, List<Move> moves) {
        return moves.stream()
                .filter(m -> m instanceof Move.PlayCard)
                .map(m -> ((Move.PlayCard) m).card())
                .toList();
    }

    private static boolean isAvoidTricks(ScoringPolicy policy) {
        return policy instanceof ScoringPolicy.TricksScorePoints
                || policy instanceof ScoringPolicy.LastTwoTricksScorePoints;
    }

    private static boolean hasAvoidTricksComponent(ScoringPolicy policy) {
        return switch (policy) {
            case ScoringPolicy.TricksScorePoints _ -> true;
            case ScoringPolicy.LastTwoTricksScorePoints _ -> true;
            case ScoringPolicy.CompositeScoringPolicy(List<ScoringPolicy> children) ->
                    children.stream().anyMatch(HeuristicBot::hasAvoidTricksComponent);
            default -> false;
        };
    }

    /**
     * Sum of penalty points this card would add to the trick-winner's score.
     * Only counts card-level policies (suit, rank, specific card). Trick-level policies
     * like {@link ScoringPolicy.TricksScorePoints} score regardless of which card is played.
     */
    private static int cardPenaltyPoints(Card card, ScoringPolicy policy) {
        return switch (policy) {
            case ScoringPolicy.SuitScoresPoints s -> card.suit() == s.suit() ? s.pointsPerCard() : 0;
            case ScoringPolicy.RankScoresPoints r -> card.rank() == r.rank() ? r.pointsPerCard() : 0;
            case ScoringPolicy.CardScoresPoints c -> card.equals(c.card()) ? c.points() : 0;
            case ScoringPolicy.CompositeScoringPolicy(List<ScoringPolicy> children) ->
                    children.stream().mapToInt(child -> cardPenaltyPoints(card, child)).sum();
            default -> 0;
        };
    }

    private static boolean isPenalty(Card card, ScoringPolicy policy) {
        return switch (policy) {
            case ScoringPolicy.SuitScoresPoints s -> card.suit() == s.suit();
            case ScoringPolicy.RankScoresPoints r -> card.rank() == r.rank();
            case ScoringPolicy.CardScoresPoints c -> card.equals(c.card());
            case ScoringPolicy.CompositeScoringPolicy(List<ScoringPolicy> children) ->
                    children.stream().anyMatch(child -> isPenalty(card, child));
            default -> false;
        };
    }

    private static Card lowest(List<Card> cards) {
        return cards.stream().min(byRank()).orElseThrow();
    }

    private static Card highest(List<Card> cards) {
        return cards.stream().max(byRank()).orElseThrow();
    }

    private static Comparator<Card> byRank() {
        return Comparator.comparingInt(c -> c.rank().getValue());
    }
}