package com.barbu.api.game.dto.stomp.gameupdate;

import com.barbu.api.game.dto.stomp.gameupdate.parts.CompletedPlayAreaDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.GameHistoryDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.GameStateDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.OutgoingMoveDTO;

/**
 * Combined public broadcast sent to all players after each move via WebSocket.
 *
 * <p>Sent to {@code /topic/games/{gameId}}.
 *
 * @param move               The move that was just applied.
 * @param gameState          The resulting game state.
 * @param history            The full history of completed contracts.
 * @param completedPlayArea  Non-null when a trick completed (type TRICK) or a Réussite
 *                           contract ended (type TABLEAU). Used by the frontend to animate
 *                           the final state before transitioning to the next phase.
 */
public record PublicGameUpdateDTO(OutgoingMoveDTO move, GameStateDTO gameState, GameHistoryDTO history, CompletedPlayAreaDTO completedPlayArea) {}