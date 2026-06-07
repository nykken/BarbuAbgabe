package org.engine.contract.reussite;

import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.contract.Contract;
import org.engine.contract.ContractState;
import org.engine.game.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Contract configuration for the Réussite (solitaire-style) contract.
 *
 * <p>Players attempt to empty their hands by building suit-based piles on the tableau.
 * Points are awarded based on the order in which players finish their hands.
 *
 * @param startingRank     The rank required to start a new suit pile. If {@code null},
 *                         the rank of the first card played determines the starting rank
 *                         for all four suit piles.
 * @param placementPoints  Scores awarded by finishing position: index 0 = first place, etc.
 * @param replayAfterRank  A rank that grants an immediate extra turn when played.
 *                         If {@code null}, no cards grant a replay.
 */
public record ReussiteContract(
        @Nullable Rank startingRank,
        List<Integer> placementPoints,
        @Nullable Rank replayAfterRank
) implements Contract {

    public ReussiteContract {
        if (placementPoints.isEmpty() || placementPoints.size() > Player.values().length) {
            throw new IllegalArgumentException(
                    String.format("placementPoints must have between 1 and %d entries, but got %d",
                            Player.values().length,
                            placementPoints.size())
            );
        }
        placementPoints = List.copyOf(placementPoints);
    }

    @Override
    public ContractState start(Player declarer, Map<Player, Hand> hands) {
        Tableau initialTableau = startingRank != null ? new Tableau(startingRank) : new Tableau();
        return new ReussiteState(this, declarer, hands, initialTableau, List.of());
    }
}
