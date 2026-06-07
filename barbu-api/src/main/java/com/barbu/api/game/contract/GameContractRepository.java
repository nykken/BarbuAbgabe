package com.barbu.api.game.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GameContractRepository extends JpaRepository<GameContractEntity, UUID> {}
