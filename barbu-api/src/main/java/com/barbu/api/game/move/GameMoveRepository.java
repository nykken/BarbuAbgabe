package com.barbu.api.game.move;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameMoveRepository extends JpaRepository<GameMoveEntity, UUID> {

    // Must be ordered: replay depends on moves being applied in order.
    List<GameMoveEntity> findByGame_IdOrderByMoveIndex(UUID gameId);
}