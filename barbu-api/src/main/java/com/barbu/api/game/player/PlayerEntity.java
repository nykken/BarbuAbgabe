package com.barbu.api.game.player;

import com.barbu.api.game.GameEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.engine.game.Player;

import java.util.UUID;

/**
 * Abstract base for all players occupying a seat in a game.
 *
 * <p>Uses JPA {@link InheritanceType#JOINED} inheritance: common columns live in
 * {@code game_players} and each subtype ({@code human_players}, {@code bot_players})
 * has its own table joined by primary key.
 *
 * <p>The unique constraint on {@code (game_id, position)} enforces that at most one
 * player can occupy any given seat in a game at a time.
 */
@Entity
@Table(
        name = "game_players",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "position"})
)
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PlayerEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameEntity game;


    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private Player position;


    protected PlayerEntity(GameEntity game, Player position) {
        this.id = UUID.randomUUID();
        this.game = game;
        this.position = position;
    }

}