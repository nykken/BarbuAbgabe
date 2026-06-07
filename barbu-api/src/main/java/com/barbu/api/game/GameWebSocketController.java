package com.barbu.api.game;

import com.barbu.api.exception.NotFoundException;
import com.barbu.api.game.dto.stomp.LobbyEventDTO;
import com.barbu.api.game.dto.stomp.gameupdate.PublicGameUpdateDTO;
import com.barbu.api.game.dto.stomp.IncomingMoveDTO;
import com.barbu.api.game.dto.stomp.PrivatePlayerStateDTO;
import org.engine.contract.IllegalMoveException;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP WebSocket controller for real-time game events.
 *
 * <p>Creating, joining and leaving a game are handled by {@link GameRestController}.
 */
@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final GameBroadcaster broadcaster;

    public GameWebSocketController(GameService gameService,
                                   GameBroadcaster broadcaster) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
    }


    /**
     * Fills empty seats with bots and broadcasts the lobby.
     * Then starts the game and broadcasts the game state.
     */
    @MessageMapping("/games/{gameId}/lobby/start")
    public void handleStart(@DestinationVariable UUID gameId, Principal principal) {
        String startingUser = principal.getName();
        broadcaster.broadcastLobby(gameId, gameService.fillWithBots(gameId, startingUser));
        gameService.startGame(gameId, startingUser, broadcaster.bundleSenderFor(gameId));
    }

    /** Validates and applies the move, then broadcasts the resulting game state to all players. */
    @MessageMapping("/games/{gameId}/move")
    public void handleMove(@DestinationVariable UUID gameId,
                           @Payload IncomingMoveDTO moveDTO,
                           Principal principal) throws IllegalMoveException, IllegalArgumentException {
        gameService.applyMove(gameId, moveDTO, principal.getName(), broadcaster.bundleSenderFor(gameId));
    }

    /** Sends errors privately to the player who triggered them. */
    @MessageExceptionHandler({IllegalMoveException.class, IllegalArgumentException.class,
                                IllegalStateException.class, NotFoundException.class})
    public void handleGameExceptions(Exception e, Principal principal, @DestinationVariable UUID gameId) {
        broadcaster.sendError(gameId, principal.getName(), e.getMessage());
    }

    /**
     * Returns the current lobby snapshot directly to the subscribing client.
     */
    @SubscribeMapping("/games/{gameId}/lobby")
    public LobbyEventDTO handleLobbySubscription(@DestinationVariable UUID gameId, Principal principal) {
        return gameService.getLobbyState(gameId);
    }

    /**
     * Returns the current public game state directly to the subscribing client.
     * Returns null if the game has not started yet.
     */
    @SubscribeMapping("/games/{gameId}/public-state")
    public PublicGameUpdateDTO handlePublicStateSubscription(@DestinationVariable UUID gameId) {
        return gameService.getGameUpdate(gameId);
    }

    /**
     * Returns the subscribing player's private state (hand + legal moves) directly to them.
     * Returns null if the game has not started yet.
     */
    @SubscribeMapping("/games/{gameId}/private-state")
    public PrivatePlayerStateDTO handlePrivateStateSubscription(@DestinationVariable UUID gameId,
                                                                Principal principal) {
        return gameService.getPrivatePlayerState(gameId, principal.getName());
    }

}