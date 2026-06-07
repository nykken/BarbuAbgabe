package com.barbu.api.game.dto.rest;

import com.barbu.api.game.player.bot.BotType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for creating a new game")
public record CreateGameRequest(
        @NotBlank
        @Schema(description = "ID of the game variant to play", example = "standard")
        String variantId,

        @Schema(description = "Bot difficulty for single-player games (EASY, MEDIUM, HARD). Defaults to MEDIUM if omitted.")
        BotType botDifficulty
) {}