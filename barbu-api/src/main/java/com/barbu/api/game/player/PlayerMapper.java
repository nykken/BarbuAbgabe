package com.barbu.api.game.player;

import com.barbu.api.game.dto.stomp.LobbyEventDTO;
import com.barbu.api.game.player.bot.BotPlayerEntity;
import com.barbu.api.game.player.human.HumanPlayerEntity;

/**
 * Converts {@link PlayerEntity} instances to their DTO representation.
 */
public class PlayerMapper {
    /**
     * Maps a player entity to a {@link LobbyEventDTO.PlayerInfoDTO}.
     * The {@code default} branch guards against new {@link PlayerEntity} subtypes
     * being added without updating this mapper.
     */
    public static LobbyEventDTO.PlayerInfoDTO mapPlayer(PlayerEntity entity) {
        return switch (entity) {
            case HumanPlayerEntity h -> new LobbyEventDTO.PlayerInfoDTO.HumanPlayer(
                    h.getPosition(), h.getUserEntity().getUsername());
            case BotPlayerEntity b -> new LobbyEventDTO.PlayerInfoDTO.BotPlayer(
                    b.getPosition(), b.getBotType(), b.getBotName());
            default -> throw new IllegalArgumentException(
                    "Unknown player entity type: " + entity.getClass().getSimpleName());
        };
    }
}