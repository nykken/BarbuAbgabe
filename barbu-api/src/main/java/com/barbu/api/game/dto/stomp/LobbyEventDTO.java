package com.barbu.api.game.dto.stomp;

import com.barbu.api.game.player.bot.BotName;
import com.barbu.api.game.player.bot.BotType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.game.Player;
import java.util.List;

public record LobbyEventDTO(
        List<PlayerInfoDTO> players
) {
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PlayerInfoDTO.HumanPlayer.class, name = "HUMAN"),
            @JsonSubTypes.Type(value = PlayerInfoDTO.BotPlayer.class, name = "BOT")
    })
    public sealed interface PlayerInfoDTO permits PlayerInfoDTO.HumanPlayer, PlayerInfoDTO.BotPlayer {
        record HumanPlayer(
                Player position,
                String username
        ) implements PlayerInfoDTO {}

        record BotPlayer(
                Player position,
                BotType botType,
                BotName botName
        ) implements PlayerInfoDTO {}
    }
}