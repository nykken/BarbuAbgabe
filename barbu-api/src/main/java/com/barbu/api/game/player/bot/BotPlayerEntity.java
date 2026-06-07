package com.barbu.api.game.player.bot;

import com.barbu.api.game.GameEntity;
import com.barbu.api.game.player.PlayerEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.engine.game.Player;

/**
 * Bot subtype of {@link PlayerEntity} in the JOINED inheritance hierarchy.
 */
@Entity
@Table(name = "bot_players")
@DiscriminatorValue("BOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BotPlayerEntity extends PlayerEntity {

    public BotPlayerEntity(GameEntity game, Player position, BotType botType, BotName botName) {
        super(game, position);
        this.botType = botType;
        this.botName = botName;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_type", nullable = false)
    private BotType botType;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_name", nullable = false)
    private BotName botName;
}