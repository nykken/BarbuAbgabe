package com.barbu.api.game.player.human;

import com.barbu.api.game.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface HumanPlayerRepository extends JpaRepository<HumanPlayerEntity, UUID> {

    /** Looks up a specific user's seat in the given game. */
    Optional<HumanPlayerEntity> findByGameIdAndUserEntity_Username(UUID gameId, String username);

    /**
     * Enforces the single-active-game rule: returns {@code true} if the user
     * is already in a game whose status is in {@code statuses}.
     */
    boolean existsByUserEntity_UsernameAndGame_StatusIn(String username, Collection<GameStatus> statuses);

    /**
     * Retrieves the user's current active game entry (to redirect on reconnect).
     */
    Optional<HumanPlayerEntity> findFirstByUserEntity_UsernameAndGame_StatusIn(String username, Collection<GameStatus> statuses);
}