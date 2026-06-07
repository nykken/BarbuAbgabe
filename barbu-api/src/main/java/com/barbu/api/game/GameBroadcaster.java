package com.barbu.api.game;

import com.barbu.api.game.dto.stomp.LobbyEventDTO;
import com.barbu.api.game.dto.stomp.PrivatePlayerStateDTO;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Centralizes all outbound STOMP routing: game state broadcasts, per-player private updates,
 * lobby events, and error delivery.
 */
@Component
public class GameBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    public GameBroadcaster(SimpMessagingTemplate messagingTemplate, SimpUserRegistry simpUserRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    /**
     * Returns a {@link Consumer} that broadcasts the {@link GameUpdateBundle} for {@code gameId}.
     */
    public Consumer<GameUpdateBundle> bundleSenderFor(UUID gameId) {
        return bundle -> broadcastGameUpdateBundle(gameId, bundle);
    }

    /** Broadcasts the public game update to all subscribers and each player's private state to their sessions. */
    public void broadcastGameUpdateBundle(UUID gameId, GameUpdateBundle bundle) {
        messagingTemplate.convertAndSend("/topic/games/" + gameId, bundle.publicUpdate());
        for (Map.Entry<String, PrivatePlayerStateDTO> entry : bundle.privateUpdates().entrySet()) {
            sendToUserSessions(
                    entry.getKey(),
                    "/queue/games/" + gameId + "/private-state",
                    entry.getValue()
            );
        }
    }

    /** Broadcasts a lobby event (player join/leave/start) to all subscribers of the game lobby topic. */
    public void broadcastLobby(UUID gameId, LobbyEventDTO lobbyEvent) {
        messagingTemplate.convertAndSend("/topic/games/" + gameId + "/lobby", lobbyEvent);
    }

    /** Sends an error message privately to {@code username}'s active sessions for the given game. */
    public void sendError(UUID gameId, String username, String message) {
        sendToUserSessions(
                username,
                "/queue/games/" + gameId + "/error",
                Map.of("error", message)
        );
    }

    /**
     * Bypasses standard Spring routing to deliver messages to users with multiple active sessions.
     * <p>
     * <b>Do NOT replace this with standard {@code convertAndSendToUser}.</b>
     * <p>
     * When Spring's {@code preservePublishOrder} is enabled, standard STOMP routing fails if a user is
     * connected from multiple tabs. Spring attempts to iterate over the user's sessions and reuse a single
     * {@code Message} object. It locks the message headers after sending to the first session, causing an
     * {@code IllegalArgumentException: Expected mutable SimpMessageHeaderAccessor} when it attempts to
     * send to the second session.
     * <p>
     * This method resolves the issue by manually iterating through the user's active sessions and creating
     * a fresh, mutable {@link SimpMessageHeaderAccessor} explicitly targeted at each session ID.
     *
     * @param username    The authenticated username of the target recipient.
     * @param destination The destination queue path (excluding the user prefix).
     * @param payload     The payload to send.
     */
    private void sendToUserSessions(String username, String destination, Object payload) {
        SimpUser simpUser = simpUserRegistry.getUser(username);

        if (simpUser == null) {
            return;
        }

        // Iterate through all of this user's sessions
        for (SimpSession session : simpUser.getSessions()) {

            // Create a new, mutable header for this specific session
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(session.getId());
            headerAccessor.setLeaveMutable(true);

            messagingTemplate.convertAndSendToUser(
                    username,
                    destination,
                    payload,
                    headerAccessor.getMessageHeaders()
            );
        }
    }
}