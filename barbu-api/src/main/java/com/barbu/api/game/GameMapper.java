package com.barbu.api.game;

import com.barbu.api.game.contract.GameContractEntity;
import com.barbu.api.game.dto.stomp.IncomingMoveDTO;
import com.barbu.api.game.dto.stomp.PrivatePlayerStateDTO;
import com.barbu.api.game.dto.stomp.gameupdate.PublicGameUpdateDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.CompletedPlayAreaDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.OutgoingMoveDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.GameHistoryDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.GameStateDTO;
import com.barbu.api.game.dto.stomp.gameupdate.parts.PlayAreaDTO;
import com.barbu.api.variants.ContractInfoDTO;
import com.barbu.api.variants.VariantInfoDTO;
import com.barbu.api.variants.VariantMapper;
import com.barbu.catalog.ContractDefinition;
import jakarta.annotation.Nullable;
import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.contract.Contract;
import org.engine.contract.reussite.ReussiteState;
import org.engine.contract.reussite.Tableau;
import org.engine.contract.trick.Trick;
import org.engine.contract.trick.TrickTakingState;
import org.engine.game.*;
import org.engine.game.state.ContractInProgress;
import org.engine.game.state.GameOver;
import org.engine.game.state.GameState;
import org.engine.game.state.WaitingForContractSelection;

import java.util.*;
import java.util.stream.Collectors;


public class GameMapper {

    public static GameSettings toGameSettings(GameEntity game) {
        List<Contract> contracts = game.getContracts().stream()
                .map(GameContractEntity::getContract)
                .toList();
        return new GameSettings(contracts, game.getDeckType(), game.getRankingOrder());
    }

    /** Maps a game's variant. Contract ids are the {@code GameContractEntity} UUIDs,
     * not catalog keys.
     * */
    public static VariantInfoDTO mapGameVariantInfo(GameEntity game) {
        List<ContractInfoDTO> contracts = game.getContracts().stream()
                .map(c -> VariantMapper.mapContractInfo(new ContractDefinition(
                        c.getId().toString(),
                        c.getDisplayName(),
                        c.getContract())))
                .toList();
        return new VariantInfoDTO(
                game.getId().toString(),
                game.getVariantName(),
                contracts,
                game.getDeckType(),
                game.getRankingOrder());
    }

    /**
     * Maps the current engine {@link GameState} to the public {@link GameStateDTO} broadcast to all clients.
     *
     * @param contractIdMap maps each engine {@link Contract} to its persisted {@link java.util.UUID},
     *                      used to identify contracts in client/server communication
     */
    public static GameStateDTO mapGameState(GameState state, Map<Contract, UUID> contractIdMap) {
        return switch (state) {
            case WaitingForContractSelection w -> {
                List<UUID> available = w.remainingContracts(w.currentDeclarer()).stream()
                        .map(c -> requireContractId(contractIdMap, c))
                        .toList();
                yield new GameStateDTO.WaitingForContractSelection(
                        w.currentDeclarer(),
                        w.cumulativeScores(),
                        countCards(w.hands()),
                        available);
            }

            case ContractInProgress c -> {
                UUID contractId = requireContractId(contractIdMap, c.activeContract().contract());
                yield new GameStateDTO.ContractInProgress(
                        c.currentDeclarer(),
                        c.currentPlayer(),
                        contractId,
                        buildPlayArea(c),
                        c.cumulativeScores(),
                        countCards(c.hands()));
            }

            case GameOver g ->
                    new GameStateDTO.GameOver(g.cumulativeScores());
        };
    }

    /**
     * Maps the current engine {@link GameState} to the per-player private state (hand + legal moves)
     * for {@code requestingPlayer}.
     *
     * <p>Legal moves are only populated when it is {@code requestingPlayer}'s turn.
     * Returns {@code null} for {@link GameOver}
     */
    @Nullable
    public static PrivatePlayerStateDTO mapPlayerState(GameState state, Player requestingPlayer) {
        return switch (state) {
            case WaitingForContractSelection w ->
                    new PrivatePlayerStateDTO(w.hands().get(requestingPlayer).cards(), null);

            case ContractInProgress c when c.currentPlayer() == requestingPlayer ->
                    new PrivatePlayerStateDTO(
                            c.hands().get(requestingPlayer).cards(),
                            Set.copyOf(c.activeContract().currentPlayerLegalCards()));

            case ContractInProgress c ->
                    new PrivatePlayerStateDTO(c.hands().get(requestingPlayer).cards(), null);

            case GameOver _ -> null;
        };
    }

    /** Maps the completed-contract history from {@link GameState} to {@link GameHistoryDTO}. */
    public static GameHistoryDTO mapHistory(GameState state, Map<Contract, UUID> contractIdMap) {
        History history = state.history();
        List<GameHistoryDTO.DeclarerTurnResultDTO> turns = Arrays.stream(Player.values())
                .map(p -> mapDeclarerTurnResult(contractIdMap, p, history))
                .filter(t -> !t.playedContracts().isEmpty())
                .toList();
        return new GameHistoryDTO(turns);
    }

    /** Converts an engine {@link Move} to its outgoing DTO form for broadcast to clients. */
    public static OutgoingMoveDTO toDTO(Move move) {
        return switch (move) {
            case Move.SelectContract m -> new OutgoingMoveDTO.SelectContract(m.declarer());
            case Move.PlayCard m -> new OutgoingMoveDTO.PlayCard(m.player(), m.card());
            case Move.Pass m -> new OutgoingMoveDTO.Pass(m.player());
            default -> throw new IllegalArgumentException(
                    "Unknown move type: " + move.getClass().getSimpleName());
        };
    }

    /**
     * Converts an incoming client {@link IncomingMoveDTO} to an engine {@link Move}.
     *
     * @param contract the pre-resolved engine {@link Contract} for a {@link IncomingMoveDTO.SelectContract},
     *                 {@code null} for all other move types
     */
    public static Move toMove(IncomingMoveDTO dto, Player player, Contract contract) {
        return switch (dto) {
            case IncomingMoveDTO.SelectContract _ -> new Move.SelectContract(player, contract);
            case IncomingMoveDTO.PlayCard m -> new Move.PlayCard(player, m.card());
            case IncomingMoveDTO.Pass _ -> new Move.Pass(player);
        };
    }

    /**
     * Assembles the full {@link PublicGameUpdateDTO} to broadcast after a move transition.
     * Called by {@link GameService} for every move, including bot moves.
     *
     * @param move      the move just applied, or {@code null} when broadcasting the initial game state on start
     * @param prevState game state before {@code move}, or {@code null} on start
     */
    public static PublicGameUpdateDTO buildPublicGameState(Map<Contract, UUID> contractIdMap,
                                                           Move move, GameState prevState, GameState newState) {
        return new PublicGameUpdateDTO(
                move == null ? null : toDTO(move),
                mapGameState(newState, contractIdMap),
                mapHistory(newState, contractIdMap),
                detectCompletedPlayArea(move, prevState, newState)
        );
    }

    /**
     * Recovers the play area (trick or tableau) that was completed by {@code move}, if any.
     *
     * <p> When a trick or Réussite tableau is completed, the engine immediately advances to the next
     * state, which no longer contains the completed area. This method reconstructs it from
     * {@code prevState} and the triggering move so the frontend can display it before it disappears.
     *
     * <p>The result is embedded in {@link PublicGameUpdateDTO} and broadcast to all clients via
     * STOMP after every move (see {@link GameService}).
     *
     * @param move      the move just applied (may be {@code null} for synthetic/initial states)
     * @param prevState game state immediately before {@code move} was applied
     * @param newState  game state immediately after {@code move} was applied
     * @return a {@link CompletedPlayAreaDTO.CompletedTrickDTO} or
     *         {@link CompletedPlayAreaDTO.CompletedTableauDTO} if this move completed a play area,
     *         or {@code null}
     */
    public static CompletedPlayAreaDTO detectCompletedPlayArea(Move move, GameState prevState, GameState newState) {
        CompletedPlayAreaDTO trick = detectCompletedTrick(move, prevState);
        if (trick != null) return trick;
        return detectCompletedTableau(move, prevState, newState);
    }

    // Checks whether this move completed a trick in a trick-taking contract.
    // Must use prevState: the completed trick is already gone from newState because the engine
    // clears it and starts the next trick as part of the same applyMove transition.
    private static CompletedPlayAreaDTO detectCompletedTrick(Move move, GameState prevState) {
        if (!(prevState instanceof ContractInProgress prevInProgress &&
                prevInProgress.activeContract() instanceof TrickTakingState prevTrickState)) {
            return null;
        }

        if (!(move instanceof Move.PlayCard(Player player, Card card))) {
            return null;
        }

        Trick completedTrick = prevTrickState.playArea().with(player, card);
        if (!completedTrick.isComplete()) {
            return null;
        }

        return new CompletedPlayAreaDTO.CompletedTrickDTO(cardsInPlayOrder(completedTrick), completedTrick.winner());
    }


    // Throwing on a non-PlayCard move is intentional: Réussite contracts only end via PlayCard,
    // so any other move reaching this point would be a logic error.
    @Nullable
    private static CompletedPlayAreaDTO detectCompletedTableau(Move move, GameState prevState, GameState newState) {
        if (newState instanceof ContractInProgress) {
            return null;
        }

        if (!(prevState instanceof ContractInProgress prevInProgress &&
                prevInProgress.activeContract() instanceof ReussiteState reussiteState)) {
            return null;
        }

        if (!(move instanceof Move.PlayCard(Player player, Card card))) {
            throw new IllegalStateException("Expected Move.PlayCard");
        }

        Tableau finalTableau = reussiteState.playArea().with(card);
        return new CompletedPlayAreaDTO.CompletedTableauDTO(finalTableau.getPiles(), player);
    }

    private static GameHistoryDTO.DeclarerTurnResultDTO mapDeclarerTurnResult(Map<Contract, UUID> contractIdMap,
                                                                              Player p, History history) {
        List<GameHistoryDTO.ContractResultDTO> results = history.finishedContractsForDeclarer(p)
                .stream()
                .map(fc -> mapContractResult(contractIdMap, fc))
                .toList();
        return new GameHistoryDTO.DeclarerTurnResultDTO(p, results);
    }

    private static GameHistoryDTO.ContractResultDTO mapContractResult(Map<Contract, UUID> contractIdMap,
                                                                      History.FinishedContract fc) {
        UUID contractId = requireContractId(contractIdMap, fc.contract());
        return new GameHistoryDTO.ContractResultDTO(contractId, fc.scores());
    }

    private static UUID requireContractId(Map<Contract, UUID> contractIdMap, Contract contract) {
        UUID id = contractIdMap.get(contract);
        if (id == null) throw new IllegalStateException("No UUID found for contract: " + contract);
        return id;
    }

    private static Map<Player, Card> cardsInPlayOrder(Trick trick) {
        Map<Player, Card> cards = new LinkedHashMap<>();
        for (Player player : trick.playOrder()) {
            trick.cardPlayedBy(player).ifPresent(card -> cards.put(player, card));
        }
        return cards;
    }

    private static Map<Player, Integer> countCards(Map<Player, Hand> hands) {
        return hands.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private static PlayAreaDTO buildPlayArea(ContractInProgress state) {
        var activeContract = state.activeContract();
        return switch (activeContract.playArea()) {
            case Trick trick ->
                    new PlayAreaDTO.TrickDTO(cardsInPlayOrder(trick), ((TrickTakingState) activeContract).tricksTaken());
            case Tableau tableau -> new PlayAreaDTO.TableauDTO(tableau.getPiles());
            default -> throw new IllegalStateException(
                    "Unknown PlayArea type: " + activeContract.playArea().getClass().getSimpleName());
        };
    }
}