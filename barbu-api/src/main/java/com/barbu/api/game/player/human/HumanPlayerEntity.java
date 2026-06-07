package com.barbu.api.game.player.human;

import com.barbu.api.game.player.PlayerEntity;
import com.barbu.api.user.UserEntity;
import com.barbu.api.game.GameEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.engine.game.Player;

/**
 * Human subtype of {@link com.barbu.api.game.player.PlayerEntity} in the JOINED inheritance hierarchy.
 * Links a seat in a game to a registered {@link com.barbu.api.user.UserEntity}.
 * <p>
 * {@code user_id} is indexed because it is frequently queried by username
 */
@Entity
@Table(name = "human_players", indexes = {
        @Index(name = "idx_human_players_user_id", columnList = "user_id")
})
@DiscriminatorValue("HUMAN")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HumanPlayerEntity extends PlayerEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    public HumanPlayerEntity(GameEntity game, Player position, UserEntity userEntity) {
        super(game, position);
        this.userEntity = userEntity;
    }
}