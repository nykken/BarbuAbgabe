package com.barbu.api.variants;

import org.engine.card.Deck;
import org.engine.game.GameSettings;

import java.util.List;

/**
 * Describes a game variant and its contracts. This same shape is returned by two different endpoints:
 *
 * <ul>
 *   <li>{@code GET /api/variants}: lists all playable Barbu variants. {@code id} and each contract's
 *       {@code id} are stable string keys (e.g. {@code "standard"}, {@code "standard_hearts"}).</li>
 *   <li>{@code GET /api/games/{id}/variant}: {@code id} is the game UUID and each contract's
 *       {@code id} is its {@code GameContractEntity} UUID, which is what the client sends back
 *       in WebSocket move messages.</li>
 * </ul>
 *
 * <p>Both are interchangeable for rendering contract rules on the frontend.
 */
public record VariantInfoDTO(
        String id,
        String displayName,
        List<ContractInfoDTO> contracts,
        Deck.Variant deckVariant,
        GameSettings.RankingOrder rankingOrder
) {}
