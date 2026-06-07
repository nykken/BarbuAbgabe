package com.barbu.api.game.dto.stomp.gameupdate.parts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.game.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GameStateDTO.WaitingForContractSelection.class,
                name = "WAITING_FOR_CONTRACT_SELECTION"),
        @JsonSubTypes.Type(value = GameStateDTO.ContractInProgress.class,
                name = "CONTRACT_IN_PROGRESS"),
        @JsonSubTypes.Type(value = GameStateDTO.GameOver.class,
                name = "GAME_OVER")
})
public sealed interface GameStateDTO
        permits GameStateDTO.WaitingForContractSelection,
                GameStateDTO.ContractInProgress,
                GameStateDTO.GameOver {

    /** The current declarer must pick a contract. All players see their hands. */
    record WaitingForContractSelection(
            Player currentDeclarer,
            Map<Player, Integer> cumulativeScores,
            Map<Player, Integer> cardCounts,
            List<UUID> availableContractIds
    ) implements GameStateDTO {}

    /**
     * A contract is being played. {@code tableState} is always non-null and reflects
     * whether it is a trick-taking round (TRICK) or a Réussite layout (TABLEAU).
     */
    record ContractInProgress(
            Player currentDeclarer,
            Player currentPlayer,
            UUID contractId,
            PlayAreaDTO tableState,
            Map<Player, Integer> cumulativeScores,
            Map<Player, Integer> cardCounts
    ) implements GameStateDTO {}

    /** All declarers have completed all their contracts. */
    record GameOver(Map<Player, Integer> finalScores) implements GameStateDTO {}
}
