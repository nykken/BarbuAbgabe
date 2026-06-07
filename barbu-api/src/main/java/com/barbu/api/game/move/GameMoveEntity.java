package com.barbu.api.game.move;

import com.barbu.api.game.GameEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.engine.game.Move;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;


/**
 * Append-only log of every move played in a game.
 * Game state is not stored directly, it is reconstructed by replaying
 * all moves for a game in sequence via GameState.replay().
 */
@Entity
@Table(name = "game_moves",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_game_moves_game_id_move_index",
                columnNames = {"game_id", "move_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameMoveEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameEntity game;

    // Per-game 0-based position, set by the application.
    // Provides stable ordering on replay
    @Column(name = "move_index", nullable = false, updatable = false)
    private int moveIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "move_json", nullable = false)
    private Move move;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    public GameMoveEntity(GameEntity game, Move move, int moveIndex) {
        this.id = UUID.randomUUID();
        this.game = game;
        this.move = move;
        this.moveIndex = moveIndex;
        this.playedAt = Instant.now();
    }
}
