package com.barbu.api.game;

import com.barbu.api.game.contract.GameContractEntity;
import com.barbu.api.game.player.bot.BotName;
import com.barbu.api.game.player.bot.BotType;
import com.barbu.api.game.player.PlayerEntity;
import com.barbu.api.game.player.human.HumanPlayerEntity;
import com.barbu.api.game.player.bot.BotPlayerEntity;
import com.barbu.api.user.UserEntity;
import com.barbu.catalog.ContractDefinition;
import com.barbu.catalog.GameVariant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.engine.card.Deck;
import org.engine.game.GameSettings;
import org.engine.game.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Persistent game record. Stores metadata and configuration only.
 * Current state is reconstructed at runtime by replaying the {@link com.barbu.api.game.move.GameMoveEntity}
 * log via {@link org.engine.game.state.GameState#replay}.
 */
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_games_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameEntity {

    @Id
    private UUID id;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlayerEntity> players;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GameContractEntity> contracts;

    @Column(name = "initial_seed", nullable = false)
    private long initialSeed;

    @Column(name = "variant_name", nullable = false)
    private String variantName;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_order", nullable = false)
    private GameSettings.RankingOrder rankingOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "deck_type", nullable = false)
    private Deck.Variant deckType;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter
    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_bot_type", nullable = false)
    private BotType defaultBotType;

    public GameEntity(GameVariant variant, long initialSeed, UserEntity creator, BotType defaultBotType) {
        this.id = UUID.randomUUID();
        this.variantName = variant.displayName();
        this.rankingOrder = variant.rankingOrder();
        this.deckType = variant.deckVariant();
        this.initialSeed = initialSeed;
        this.status = GameStatus.WAITING_FOR_PLAYERS;
        this.defaultBotType = defaultBotType != null ? defaultBotType : BotType.MEDIUM;
        this.createdAt = Instant.now();
        this.players = new ArrayList<>();
        this.contracts = new ArrayList<>();
        for (ContractDefinition cd : variant.contracts()) {
            this.contracts.add(new GameContractEntity(this, cd.displayName(), cd.contract()));
        }
        addHumanPlayer(creator);
    }
    
    public void addHumanPlayer(UserEntity userEntity) {
        Player seat = nextFreeSeat();
        HumanPlayerEntity humanPlayer = new HumanPlayerEntity(this, seat, userEntity);
        players.add(humanPlayer);
    }

    /** Adds a bot at the next available seat. */
    public void addBotPlayer(BotType type) {
        Player seat = nextFreeSeat();
        BotName botName = getAvailableBotName();
        BotPlayerEntity botPlayer = new BotPlayerEntity(this, seat, type, botName);
        players.add(botPlayer);
    }

    /**
     * Adds a bot at a specific seat. Used when replacing a human player who left mid-game,
     * so the replacement occupies the same position.
     */
    public void addBotPlayer(BotType type, Player seat) {
        BotName botName = getAvailableBotName();
        BotPlayerEntity botPlayer = new BotPlayerEntity(this, seat, type, botName);
        this.players.add(botPlayer);
    }

    private BotName getAvailableBotName() {
        EnumSet<BotName> usedNames = players.stream()
                .filter(player -> player instanceof BotPlayerEntity)
                .map(player -> ((BotPlayerEntity) player).getBotName())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BotName.class)));

        EnumSet<BotName> available = EnumSet.complementOf(usedNames);
        if (available.isEmpty()) {
            throw new IllegalStateException("All bot names are already in use in this game");
        }
        List<BotName> availableList = new ArrayList<>(available);
        Collections.shuffle(availableList, ThreadLocalRandom.current());
        return availableList.getFirst();
    }


    private Player nextFreeSeat() {
        Set<Player> taken = EnumSet.noneOf(Player.class);
        for (PlayerEntity player : players) taken.add(player.getPosition());
        List<Player> free = new ArrayList<>();
        for (Player player : Player.values()) {
            if (!taken.contains(player)) free.add(player);
        }
        if (free.isEmpty()) throw new IllegalStateException("Game " + id + " is full");
        Collections.shuffle(free);
        return free.getFirst();
    }

    public List<HumanPlayerEntity> getHumanPlayers() {
        return players.stream()
                .filter(HumanPlayerEntity.class::isInstance)
                .map(HumanPlayerEntity.class::cast)
                .toList();
    }

    public Optional<HumanPlayerEntity> findHumanByUsername(String username) {
        return getHumanPlayers().stream()
                .filter(player -> player.getUserEntity().getUsername().equals(username))
                .findFirst();
    }

    public Map<org.engine.contract.Contract, UUID> getContractIdMap() {
        return contracts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        GameContractEntity::getContract, GameContractEntity::getId));
    }

    public void markFinished() {
        this.status = GameStatus.FINISHED;
        this.finishedAt = Instant.now();
    }
}
