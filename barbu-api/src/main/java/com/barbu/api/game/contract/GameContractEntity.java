package com.barbu.api.game.contract;

import com.barbu.api.game.GameEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.engine.contract.Contract;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;


/**
 * Persists one row per contract per game, with a stable UUID.
 * The variant catalog (GameVariant) is only used at game creation to populate
 * these rows. Contract identity is never derived from it afterwards.
 */
@Entity
@Table(name = "game_contracts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "display_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameContractEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameEntity game;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // JSON column: Contract is polymorphic and always loaded whole, never queried by fields,
    // normalization would add join cost with little benefit.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contract_json", nullable = false)
    private Contract contract;

    public GameContractEntity(GameEntity game, String displayName, Contract contract) {
        this.id = UUID.randomUUID();
        this.game = game;
        this.displayName = displayName;
        this.contract = contract;
    }
}
