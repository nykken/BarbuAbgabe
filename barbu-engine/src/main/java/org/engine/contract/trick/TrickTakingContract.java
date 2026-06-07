package org.engine.contract.trick;

import org.engine.card.Hand;
import org.engine.contract.Contract;
import org.engine.contract.ContractState;
import org.engine.game.Player;

import java.util.List;
import java.util.Map;

/**
 * Contract configuration for a trick-taking contract.
 *
 * <p>Parameterised by a {@link ScoringPolicy} (scoring rules) and a
 * {@link LeadRestriction} (what the lead player may play on the first card of a trick).
 */
public record TrickTakingContract(
        ScoringPolicy scoringPolicy,
        LeadRestriction leadRestriction
) implements Contract {

    @Override
    public ContractState start(Player declarer, Map<Player, Hand> hands) {
        return new TrickTakingState(this, declarer, hands);
    }

    /**
     * Merges multiple trick-taking contracts into one by composing their scoring policies
     * and lead restrictions.
     *
     * @param parts the contracts to combine; must not be empty
     * @return a single contract whose scoring and lead rules are the union of all parts
     */
    public static TrickTakingContract combine(List<TrickTakingContract> parts) {
        var scoring = new ScoringPolicy.CompositeScoringPolicy(
                parts.stream().map(TrickTakingContract::scoringPolicy).toList()
        );
        var leads = new LeadRestriction.Composite(
                parts.stream().map(TrickTakingContract::leadRestriction).toList()
        );
        return new TrickTakingContract(scoring, leads);
    }
}
