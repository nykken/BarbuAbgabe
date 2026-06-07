package org.engine.game;

import org.engine.card.Deck;
import org.engine.contract.Contract;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for a full game session.
 */
public record GameSettings(List<Contract> contracts,
                           Deck.Variant deckVariant,
                           RankingOrder rankingOrder) {

    public enum RankingOrder {
        LOWEST_SCORE_WINS,
        HIGHEST_SCORE_WINS
    }

    public GameSettings(List<Contract> contracts,
                        Deck.Variant deckVariant,
                        RankingOrder rankingOrder) {
        this.contracts = List.copyOf(contracts);
        this.deckVariant = deckVariant;
        this.rankingOrder = rankingOrder;
        enforceUniqueContracts(this.contracts);
    }

    /** Validates that no contract object appears twice (uses record structural equality). */
    private static void enforceUniqueContracts(List<Contract> contracts) {
        Set<Contract> seen = new HashSet<>();
        for (Contract c : contracts) {
            if (!seen.add(c)) {
                throw new IllegalArgumentException("Duplicate contract in settings: " + c);
            }
        }
    }
}
