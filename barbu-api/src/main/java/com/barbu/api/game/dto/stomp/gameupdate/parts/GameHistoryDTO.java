package com.barbu.api.game.dto.stomp.gameupdate.parts;

import org.engine.game.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the historical log of the game.
 * Used by the frontend to render the scoresheet.
 *
 * @param turns A chronological list of every declarer's turn and the contracts they played.
 */
public record GameHistoryDTO(List<DeclarerTurnResultDTO> turns) {
    public record DeclarerTurnResultDTO(Player declarer, List<ContractResultDTO> playedContracts) {}

    public record ContractResultDTO(UUID contractId, Map<Player, Integer> scores) {}
}
