package org.engine.ai;

import org.engine.game.Move;
import org.engine.game.state.ActiveGameState;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Bot that mixes two policies  : on each decision it delegates to {@code primary}
 * with probability {@code primaryWeight}, otherwise to {@code fallback}.
 *
 * <p>Useful as a simulation policy that is mostly heuristic but occasionally random, adding
 * exploration noise to playouts.
 */
public class MixedPolicyBot extends Bot {

    /** Policy chosen with probability {@link #primaryWeight}. */
    private final Bot primary;
    /** Policy chosen with probability {@code 1 - primaryWeight}. */
    private final Bot fallback;
    /** Probability of delegating to {@link #primary}. */
    private final double primaryWeight;

    public MixedPolicyBot(Bot primary, Bot fallback, double primaryWeight) {
        this.primary = primary;
        this.fallback = fallback;
        this.primaryWeight = primaryWeight;
    }

    @Override
    public String describe() {
        return String.format("Mixed(%.0f%%:%s+%s)", primaryWeight * 100, primary.describe(), fallback.describe());
    }

    @Override
    protected Move search(ActiveGameState state) {
        return ThreadLocalRandom.current().nextDouble() < primaryWeight
                ? primary.chooseMove(state)
                : fallback.chooseMove(state);
    }
}