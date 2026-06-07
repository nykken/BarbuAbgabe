package com.barbu.api.game;

import com.barbu.api.game.dto.rest.GameIdResponse;
import com.barbu.api.variants.VariantInfoDTO;
import com.barbu.api.game.dto.rest.CreateGameRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Games",
        description = "Game creation, join, and state recovery. Other lobby actions and gameplay are handled over WebSocket.")
@RestController
@RequestMapping("/api/games")
public class GameRestController {

    private final GameService gameService;
    private final GameBroadcaster broadcaster;

    public GameRestController(GameService gameService, GameBroadcaster broadcaster) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
    }

    @Operation(summary = "Create a game",
            description = "Creates a new game and seats the creator. Returns the game ID.")
    @PostMapping
    public ResponseEntity<GameIdResponse> createGame(
            @Valid @RequestBody CreateGameRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID gameId = gameService.createGame(request.variantId(), userDetails.getUsername(), request.botDifficulty());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GameIdResponse(gameId));
    }


    @Operation(summary = "Join a game",
            description = "Seats the authenticated user in a waiting game and broadcasts the updated lobby.")
    @PostMapping("/{gameId}/join")
    public ResponseEntity<Void> joinGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        gameService.joinGame(gameId, username);
        broadcaster.broadcastLobby(gameId, gameService.getLobbyState(gameId));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Leave a game",
            description = "Removes the authenticated user from the game and broadcasts the resulting lobby/game state. " +
                    "If the game is in progress the seat is replaced by a bot. Returns 204.")
    @PostMapping("/{gameId}/leave")
    public ResponseEntity<Void> leaveGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        gameService.leaveGame(gameId, userDetails.getUsername(),
                        broadcaster.bundleSenderFor(gameId))
                .ifPresent(lobbyEvent -> broadcaster.broadcastLobby(gameId, lobbyEvent));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get variant info for a game",
            description = "Returns the rules snapshot stored at game creation time. " +
                    "Independent of the current variant catalog.")
    @GetMapping("/{gameId}/variant")
    public ResponseEntity<VariantInfoDTO> getGameVariantInfo(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getGameVariantInfo(gameId));
    }


    @Operation(summary = "Find active game for user",
            description = "Returns the ID of the game the user is currently in (Lobby or In-Progress). " +
                    "Returns 204 if the user has no active game.")
    @GetMapping("/current")
    public ResponseEntity<GameIdResponse> getCurrentGame(@AuthenticationPrincipal UserDetails userDetails) {
        return gameService.findActiveGame(userDetails.getUsername())
                .map(gameId -> ResponseEntity.ok(new GameIdResponse(gameId)))
                .orElse(ResponseEntity.noContent().build());
    }
}