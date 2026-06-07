package org.engine.game;

import org.engine.contract.Contract;
import org.engine.contract.ContractState;

import java.util.*;

/**
 * The completed-contract history of a Barbu game.
 *
 * <p>Maps each declarer to their contracts in play order.
 * Immutable: {@link #with} returns a new instance.
 */
public final class History {
    private final Map<Player, List<FinishedContract>> rounds;

    private History(Map<Player, List<FinishedContract>> rounds) {
        EnumMap<Player, List<FinishedContract>> copy = new EnumMap<>(Player.class);
        rounds.forEach((p, contracts) -> copy.put(p, List.copyOf(contracts)));
        this.rounds = Collections.unmodifiableMap(copy);
    }

    /** Creates a new, empty history. */
    public static History create() {
        return new History(new EnumMap<>(Player.class));
    }

    /** Returns a new history with {@code contract} appended for {@code declarer}. */
    public History with(Player declarer, ContractState contract) {
        EnumMap<Player, List<FinishedContract>> next = new EnumMap<>(Player.class);
        next.putAll(rounds);

        List<FinishedContract> updated = new ArrayList<>(next.getOrDefault(declarer, List.of()));
        updated.add(new FinishedContract(contract.contract(), contract.scores()));
        next.put(declarer, updated);

        return new History(next);
    }

    /**
     * Returns the contracts played by {@code declarer} in play order.
     * Returns an empty list if {@code declarer} has not yet played any contracts.
     */
    public List<FinishedContract> finishedContractsForDeclarer(Player declarer) {
        return rounds.getOrDefault(declarer, List.of());
    }

    @Override
    public String toString() {
        if (rounds.isEmpty()) return "History: no completed contracts";
        StringBuilder sb = new StringBuilder("History:\n");
        for (Player p : Player.values()) {
            List<FinishedContract> contracts = finishedContractsForDeclarer(p);
            if (!contracts.isEmpty()) {
                sb.append("  ").append(p.name()).append(":\n");
                contracts.forEach(fc ->
                        sb.append("    ").append(fc.contract())
                          .append(" scores=").append(fc.scores()).append("\n"));
            }
        }
        sb.append("  TOTAL: ").append(cumulativeScores());
        return sb.toString();
    }

    /** Returns per-player scores summed across all completed contracts. */
    public Map<Player, Integer> cumulativeScores() {
        Map<Player, Integer> totals = new EnumMap<>(Player.class);
        Arrays.stream(Player.values()).forEach(p -> totals.put(p, 0));
        rounds.values().stream()
                .flatMap(List::stream)
                .forEach(c -> c.scores().forEach((p, score) -> totals.merge(p, score, Integer::sum)));
        return Collections.unmodifiableMap(totals);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof History other && rounds.equals(other.rounds);
    }

    @Override
    public int hashCode() {
        return rounds.hashCode();
    }

    /**
     * An immutable summary of a completed contract: which contract was played
     * and what scores resulted.
     */
    public record FinishedContract(Contract contract, Map<Player, Integer> scores) {
        public FinishedContract {
            scores = Map.copyOf(scores);
        }
    }
}