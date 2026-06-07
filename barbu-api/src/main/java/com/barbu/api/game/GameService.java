package com.barbu.api.game;

import com.barbu.api.game.dto.stomp.LobbyEventDTO;
import com.barbu.api.game.dto.stomp.gameupdate.PublicGameUpdateDTO;
import com.barbu.api.game.dto.stomp.PrivatePlayerStateDTO;
import com.barbu.api.game.contract.GameContractEntity;
import com.barbu.api.game.move.GameMoveEntity;
import com.barbu.api.game.move.GameMoveRepository;
import com.barbu.api.game.player.*;
import com.barbu.api.game.player.bot.BotPlayerEntity;
import com.barbu.api.game.player.bot.BotType;
import com.barbu.api.game.player.human.HumanPlayerEntity;
import com.barbu.api.game.player.human.HumanPlayerRepository;
import com.barbu.api.user.UserEntity;
import com.barbu.api.user.UserRepository;
import com.barbu.api.game.dto.stomp.IncomingMoveDTO;
import com.barbu.catalog.GameCatalog;
import com.barbu.catalog.GameVariant;
import com.barbu.api.variants.VariantInfoDTO;
import com.barbu.api.exception.NotFoundException;
import jakarta.annotation.Nullable;
import org.engine.contract.Contract;
import org.engine.contract.IllegalMoveException;
import org.engine.game.Move;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.GameOver;
import org.engine.game.state.GameState;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrator for all game operations.
 *
 * <p>No mutable game state is stored in the database, only the ordered list of {@link GameMoveEntity} rows.
 * Current state is reconstructed by replaying those moves via {@link GameState#replay}.</p>
 *
 * <p>The methods ({@code applyMove}, {@code startGame} and {@code leaveGame}) use {@link TransactionTemplate}
 * rather than {@code @Transactional} so that each move (human or bot) is committed and broadcast independently.</p>
 */
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository moveRepository;
    private final UserRepository userRepository;
    private final HumanPlayerRepository humanPlayerRepository;
    private final PlayerRepository playerRepository;
    private final TransactionTemplate tx;

    private static final Set<GameStatus> ACTIVE_STATUSES =
            Set.of(GameStatus.WAITING_FOR_PLAYERS, GameStatus.IN_PROGRESS);

    public GameService(GameRepository gameRepository,
                       GameMoveRepository moveRepository,
                       UserRepository userRepository,
                       HumanPlayerRepository humanPlayerRepository,
                       PlayerRepository playerRepository,
                       PlatformTransactionManager txManager) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
        this.humanPlayerRepository = humanPlayerRepository;
        this.playerRepository = playerRepository;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Snapshot of game context extracted inside a transaction so it can be used
     * across transaction boundaries without holding a JPA session open.
     * Bot steps read from this rather than reloading the entity on each iteration.
     */
    record GameContext(
            UUID gameId,
            Map<String, Player> humanPositions,
            Map<Contract, UUID> contractIdMap,
            Map<Player, BotType> botTypes
    ) {}

    // ── public methods ─────────────────────────────────────────────────────────

    /**
     * Creates a new game for {@code creatorUsername} using the given variant.
     *
     * @param botDifficulty the bot type used for all bot additions in this game:
     *                      fill-with-bots before start, and seat replacements when a human leaves mid-game.
     *                      Defaults to {@link BotType#MEDIUM} if {@code null}.
     * @return the new game's ID
     */
    @Transactional
    public UUID createGame(String variantId, String creatorUsername, BotType botDifficulty) {
        requireNotInActiveGame(creatorUsername);
        GameVariant variant = GameCatalog.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown variant: " + variantId));
        long seed = System.currentTimeMillis();
        UserEntity creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new NotFoundException("User not found: " + creatorUsername));
        GameEntity game = new GameEntity(variant, seed, creator, botDifficulty);
        gameRepository.save(game);
        return game.getId();
    }

    /**
     * Adds {@code username} to the game's lobby.
     * Throws if the user is already in another active game, the game is full, or is no longer accepting players.
     */
    @Transactional
    public void joinGame(UUID gameId, String username) {
        requireNotInActiveGame(username);
        GameEntity game = requireGame(gameId);
        if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new IllegalArgumentException("Game " + gameId + " is not accepting players");
        }
        if (game.getPlayers().size() >= Player.values().length) {
            throw new IllegalStateException("Game " + gameId + " is full");
        }
        UserEntity joiningUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        game.addHumanPlayer(joiningUser);
        gameRepository.save(game);
    }

    /**
     * Removes {@code username} from the game and handles the effects.
     *
     * <ul>
     *   <li>Lobby: player is removed; if no humans remain, the game is abandoned.</li>
     *   <li>In-progress: the seat is immediately filled by a bot; if no humans remain the
     *       game is abandoned, otherwise bot turns run after the transaction commits.</li>
     * </ul>
     */
    public Optional<LobbyEventDTO> leaveGame(UUID gameId, String username, Consumer<GameUpdateBundle> onGameUpdateBundle) {
        LeaveResult result = leaveGameTx(gameId, username);
        if (result.context() != null) {
            runBotSteps(result.context(), result.state(), result.nextMoveIndex(), onGameUpdateBundle);
        }
        return result.lobbyEvent();
    }

    @Transactional(readOnly = true)
    public LobbyEventDTO getLobbyState(UUID gameId) {
        return buildLobbyState(gameId);
    }

    @Transactional(readOnly = true)
    public VariantInfoDTO getGameVariantInfo(UUID gameId) {
        return GameMapper.mapGameVariantInfo(requireGame(gameId));
    }

    /**
     * Returns the ID of the active game {@code username} is currently seated in, if any.
     * Used by the frontend on page load to reconnect a user to their ongoing game.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveGame(String username) {
        return humanPlayerRepository
                .findFirstByUserEntity_UsernameAndGame_StatusIn(username, ACTIVE_STATUSES)
                .map(p -> p.getGame().getId());
    }

    /**
     * Validates and persists a human move, then drives any consecutive bot turns.
     * Throws {@link org.engine.contract.IllegalMoveException} (rolls back the transaction) if the move is illegal.
     *
     * @param onGameUpdateBundle called outside the DB transaction for each broadcast snapshot
     */
    public void applyMove(UUID gameId, IncomingMoveDTO incomingMoveDTO, String username,
                          Consumer<GameUpdateBundle> onGameUpdateBundle) {
        MoveResult result = applyMoveTx(gameId, incomingMoveDTO, username);
        onGameUpdateBundle.accept(result.bundle());
        runBotSteps(result.context(), result.newState(), result.nextMoveIndex(), onGameUpdateBundle);
    }

    /**
     * Transitions the game from WAITING_FOR_PLAYERS to IN_PROGRESS, then drives any bot turns that open.
     * Requires the game to have exactly 4 players seated.
     *
     * @param onGameUpdateBundle called outside the DB transaction for each state snapshot to broadcast:
     *                 first the initial game state, then any opening bot moves
     */
    public void startGame(UUID gameId, String username, Consumer<GameUpdateBundle> onGameUpdateBundle) {
        StartResult result = startGameTx(gameId, username);
        onGameUpdateBundle.accept(result.bundle());
        runBotSteps(result.context(), result.initialState(), result.nextMoveIndex(), onGameUpdateBundle);
    }

    /**
     * Fills any empty seats with bots using the game's configured default bot type.
     * The calling user must already be seated in the game.
     * Returns the updated lobby state for broadcasting.
     */
    @Transactional
    public LobbyEventDTO fillWithBots(UUID gameId, String username) {
        GameEntity game = requireGame(gameId);

        if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game " + gameId + " is not accepting players");
        }

        game.findHumanByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + username + " is not a player in game " + gameId));

        while (game.getPlayers().size() < Player.values().length) {
            game.addBotPlayer(game.getDefaultBotType());
        }

        gameRepository.saveAndFlush(game);

        return buildLobbyState(gameId);
    }

    /**
     * Returns the current public game state for subscription snapshots.
     * Returns {@code null} if the game has not started yet or was abandoned before starting.
     */
    @Nullable
    @Transactional(readOnly = true)
    public PublicGameUpdateDTO getGameUpdate(UUID gameId) {
        GameEntity game = requireGame(gameId);
        if (game.getStatus() == GameStatus.WAITING_FOR_PLAYERS
                || game.getStatus() == GameStatus.ABANDONED) return null;
        GameState state = replay(game);
        return GameMapper.buildPublicGameState(game.getContractIdMap(), null, null, state);
    }

    /**
     * Returns the private state (hand + legal moves) for {@code username} in the given game.
     * Returns {@code null} if the game has not started yet.
     */
    @Nullable
    @Transactional(readOnly = true)
    public PrivatePlayerStateDTO getPrivatePlayerState(UUID gameId, String username) {
        GameEntity game = requireGame(gameId);
        if (game.getStatus() == GameStatus.WAITING_FOR_PLAYERS) return null;
        Player position = game.findHumanByUsername(username)
                .map(HumanPlayerEntity::getPosition)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + username + " is not seated in game " + gameId));
        GameState state = replay(game);
        return GameMapper.mapPlayerState(state, position);
    }

    // ── private helpers ─────────────────────────────────────────────────────────

    private record LeaveResult(Optional<LobbyEventDTO> lobbyEvent, GameContext context, GameState state, int nextMoveIndex) {}
    private record MoveResult(GameContext context, GameState newState, int nextMoveIndex, GameUpdateBundle bundle) {}
    private record StartResult(GameContext context, GameState initialState, int nextMoveIndex, GameUpdateBundle bundle) {}

    private LeaveResult leaveGameTx(UUID gameId, String username) {
        return tx.execute(_ -> {
            GameEntity game = requireGame(gameId);
            HumanPlayerEntity player = game.findHumanByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User " + username + " is not in game " + gameId));
            return switch (game.getStatus()) {
                case WAITING_FOR_PLAYERS -> handleLeaveWaiting(game, player);
                case IN_PROGRESS         -> handleLeaveInProgress(game, player);
                case FINISHED, ABANDONED -> new LeaveResult(Optional.empty(), null, null, 0);
            };
        });
    }

    private LeaveResult handleLeaveWaiting(GameEntity game, HumanPlayerEntity player) {
        game.getPlayers().remove(player);
        if (game.getHumanPlayers().isEmpty()) {
            game.setStatus(GameStatus.ABANDONED);
            gameRepository.save(game);
            return new LeaveResult(Optional.empty(), null, null, 0);
        }
        gameRepository.save(game);
        return new LeaveResult(Optional.of(buildLobbyState(game.getId())), null, null, 0);
    }

    private LeaveResult handleLeaveInProgress(GameEntity game, HumanPlayerEntity player) {
        Player position = player.getPosition();
        // Flush the removal before inserting the bot: the seat has a unique
        // constraint, so the DELETE must reach the DB before the INSERT.
        game.getPlayers().remove(player);
        gameRepository.saveAndFlush(game);

        if (game.getHumanPlayers().isEmpty()) {
            game.setStatus(GameStatus.ABANDONED);
            gameRepository.save(game);
            return new LeaveResult(Optional.empty(), null, null, 0);
        }

        game.addBotPlayer(game.getDefaultBotType(), position);
        gameRepository.saveAndFlush(game);
        List<Move> moves = moveLog(game.getId());
        GameState state = GameState.replay(GameMapper.toGameSettings(game), game.getInitialSeed(), moves);
        return new LeaveResult(Optional.of(buildLobbyState(game.getId())), extractContext(game), state, moves.size());
    }

    private MoveResult applyMoveTx(UUID gameId, IncomingMoveDTO incomingMoveDTO, String username) {
        return tx.execute(_ -> {
            GameEntity game = requireGame(gameId);
            if (game.getStatus() != GameStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("Game " + gameId + " is not in progress");
            }
            Player player = game.findHumanByUsername(username)
                    .map(HumanPlayerEntity::getPosition)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User " + username + " is not seated in game " + gameId));
            Map<UUID, Contract> contractByUuid = game.getContracts().stream()
                    .collect(Collectors.toMap(GameContractEntity::getId, GameContractEntity::getContract));
            Contract contract = incomingMoveDTO instanceof IncomingMoveDTO.SelectContract m
                    ? contractByUuid.get(UUID.fromString(m.contractId()))
                    : null;
            Move move = GameMapper.toMove(incomingMoveDTO, player, contract);
            List<Move> moves = moveLog(gameId);
            GameState state = GameState.replay(GameMapper.toGameSettings(game), game.getInitialSeed(), moves);
            if (!(state instanceof ActiveGameState active)) {
                throw new IllegalStateException("Game is already over");
            }
            GameState newState = active.applyMove(move); // throws if move invalid
            int nextIndex = moves.size();
            try {
                moveRepository.saveAndFlush(new GameMoveEntity(game, move, nextIndex));
            } catch (DataIntegrityViolationException e) {
                throw new IllegalMoveException("Move rejected: game state changed concurrently");
            }
            GameContext context = extractContext(game);
            return new MoveResult(context, newState, nextIndex + 1, buildBundleFromContext(context, move, state, newState));
        });
    }

    private StartResult startGameTx(UUID gameId, String username) {
        return tx.execute(_ -> {
            GameEntity game = requireGame(gameId);
            game.findHumanByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User " + username + " is not a player in game " + gameId));

            if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
                throw new IllegalArgumentException("Game " + gameId + " is not waiting for players");
            }
            int playersInGame = game.getPlayers().size();
            if (playersInGame != Player.values().length) {
                throw new IllegalArgumentException("Cannot start game with " + playersInGame + " players");
            }

            game.setStatus(GameStatus.IN_PROGRESS);
            gameRepository.save(game);
            GameState initialState = replay(game);
            GameContext context = extractContext(game);
            return new StartResult(context, initialState, 0, buildBundleFromContext(context, null, null, initialState));
        });
    }

    /**
     * Drives consecutive bot turns until a human must play or the game ends.
     * Each bot move is persisted and broadcast individually.
     */
    private void runBotSteps(GameContext context, GameState state, int startIndex,
                             Consumer<GameUpdateBundle> onGameUpdateBundle) {
        GameState current = state;
        int idx = startIndex;
        while (current instanceof ActiveGameState active
                && context.botTypes().containsKey(active.currentPlayer())) {
            BotType type = context.botTypes().get(active.currentPlayer());
            Move move = type.createBot().chooseMove(active);
            GameState next = active.applyMove(move);
            GameState prev = current;
            int moveIndex = idx;

            tx.execute(status -> {
                moveRepository.save(new GameMoveEntity(gameRepository.getReferenceById(context.gameId()), move, moveIndex));
                if (next instanceof GameOver) markFinished(context.gameId());
                return null;
            });

            idx++;
            onGameUpdateBundle.accept(buildBundleFromContext(context, move, prev, next));
            current = next;
        }
    }

    /** Extracts the data we need from the game entity so we don't have to reload it from the DB on each bot step. */
    private GameContext extractContext(GameEntity game) {
        Map<String, Player> humanPositions = new HashMap<>();
        Map<Player, BotType> botTypes = new EnumMap<>(Player.class);
        for (PlayerEntity gp : game.getPlayers()) {
            if (gp instanceof HumanPlayerEntity human) {
                humanPositions.put(human.getUserEntity().getUsername(), human.getPosition());
            } else if (gp instanceof BotPlayerEntity bot) {
                botTypes.put(bot.getPosition(), bot.getBotType());
            }
        }
        return new GameContext(game.getId(), humanPositions, game.getContractIdMap(), botTypes);
    }

    private LobbyEventDTO buildLobbyState(UUID gameId) {
        var playerInfo = playerRepository.findByGameId(gameId).stream()
                .map(PlayerMapper::mapPlayer)
                .toList();
        return new LobbyEventDTO(playerInfo);
    }

    /** {@code move} and {@code prevState} may be null when broadcasting the initial game state (on start). */
    private GameUpdateBundle buildBundleFromContext(GameContext context, Move move, GameState prevState, GameState newState) {
        Map<String, PrivatePlayerStateDTO> privateUpdates = new HashMap<>();
        for (var e : context.humanPositions().entrySet()) {
            privateUpdates.put(e.getKey(), GameMapper.mapPlayerState(newState, e.getValue()));
        }
        PublicGameUpdateDTO publicUpdate = new PublicGameUpdateDTO(
                move == null ? null : GameMapper.toDTO(move),
                GameMapper.mapGameState(newState, context.contractIdMap()),
                GameMapper.mapHistory(newState, context.contractIdMap()),
                GameMapper.detectCompletedPlayArea(move, prevState, newState)
        );
        return new GameUpdateBundle(publicUpdate, privateUpdates);
    }

    private void markFinished(UUID gameId) {
        GameEntity game = gameRepository.getReferenceById(gameId);
        game.markFinished();
        gameRepository.save(game);
    }

    private GameState replay(GameEntity game) {
        return GameState.replay(GameMapper.toGameSettings(game), game.getInitialSeed(), moveLog(game.getId()));
    }

    /** The ordered move log for a game; its size is the index the next appended move should use. */
    private List<Move> moveLog(UUID gameId) {
        return moveRepository.findByGame_IdOrderByMoveIndex(gameId)
                .stream()
                .map(GameMoveEntity::getMove)
                .toList();
    }

    private void requireNotInActiveGame(String username) {
        if (humanPlayerRepository.existsByUserEntity_UsernameAndGame_StatusIn(username, ACTIVE_STATUSES)) {
            throw new IllegalStateException("User " + username + " is already in an active game");
        }
    }

    private GameEntity requireGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
    }
}