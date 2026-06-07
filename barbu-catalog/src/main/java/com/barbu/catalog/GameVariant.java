package com.barbu.catalog;

import org.engine.card.Deck;
import org.engine.contract.Contract;
import org.engine.game.GameSettings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A named, complete game configuration that lives in the catalog layer.
 *
 * <p>The engine receives a plain {@link GameSettings} (no names, no catalog references).
 * Use {@link #toGameSettings()} to extract one.
 *
 * @param id          Stable string key stored in the database.
 * @param displayName Human-readable name sent to the frontend.
 * @param contracts   Ordered list of contract definitions for this variant. Display names must be unique.
 * @param deckVariant Which deck to use.
 * @param rankingOrder Whether lower or higher scores win.
 */
public record GameVariant(
        String id,
        String displayName,
        List<ContractDefinition> contracts,
        Deck.Variant deckVariant,
        GameSettings.RankingOrder rankingOrder
) {
    public GameVariant {
        contracts = List.copyOf(contracts);
        Set<String> seen = new HashSet<>();
        for (ContractDefinition cd : contracts)
            if (!seen.add(cd.id()))
                throw new IllegalArgumentException(
                        "Duplicate contract id in variant '" + id + "': " + cd.id());
    }

    /** Builds the engine's {@link GameSettings} by extracting the raw {@link Contract} objects. */
    public GameSettings toGameSettings() {
        return new GameSettings(
                contracts.stream().map(ContractDefinition::contract).toList(),
                deckVariant,
                rankingOrder
        );
    }

    /**
     * Finds the {@link ContractDefinition} whose engine contract equals the given instance.
     * Works because contract implementations are records (structural equality).
     */
    public Optional<ContractDefinition> findByContract(Contract contract) {
        return contracts.stream()
                .filter(cd -> cd.contract().equals(contract))
                .findFirst();
    }

    /** Returns a map of id → engine Contract, used for deserializing incoming moves. */
    public Map<String, Contract> contractById() {
        return contracts.stream()
                .collect(Collectors.toMap(ContractDefinition::id, ContractDefinition::contract));
    }
}