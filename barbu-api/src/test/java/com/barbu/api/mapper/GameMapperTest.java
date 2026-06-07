package com.barbu.api.mapper;//package com.barbu.api.mapper;
//
//import com.barbu.api.catalog.GameCatalog;
//import com.barbu.api.catalog.GameVariant;
//import com.barbu.api.dto.*;
//import org.engine.ai.FirstLegalMoveBot;
//import org.engine.card.Card;
//import org.engine.game.*;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class GameMapperTest {
//
//    private static final GameVariant VARIANT = GameCatalog.STANDARD;
//    private static final GameSettings SETTINGS = VARIANT.toGameSettings();
//    private static final long SEED = 42L;
//
//    // -------------------------------------------------------------------------
//    // Helpers
//    // -------------------------------------------------------------------------
//
//    private WaitingForContractSelection startingState() {
//        return (WaitingForContractSelection) GameState.newGame(SETTINGS, SEED);
//    }
//
//    /** Selects the first contract (HEARTS) for NORTH → ContractInProgress. */
//    private ContractInProgress contractInProgress() {
//        WaitingForContractSelection waiting = startingState();
//        return (ContractInProgress) waiting.applyMove(
//                new Move.SelectContract(Player.NORTH, SETTINGS.contracts().get(0)));
//    }
//
//    /** Selects the REUSSITE contract for NORTH → ContractInProgress with a Tableau. */
//    private ContractInProgress reussiteInProgress() {
//        WaitingForContractSelection waiting = startingState();
//        return (ContractInProgress) waiting.applyMove(
//                new Move.SelectContract(Player.NORTH, GameCatalog.reu));
//    }
//
//    /** Plays a full first contract using FirstLegalMoveBot → history has one entry. */
//    private GameState oneContractFinished() {
//        GameState state = GameState.newGame(SETTINGS, SEED);
//        FirstLegalMoveBot[] bots = {
//                new FirstLegalMoveBot(Player.NORTH),
//                new FirstLegalMoveBot(Player.EAST),
//                new FirstLegalMoveBot(Player.SOUTH),
//                new FirstLegalMoveBot(Player.WEST)
//        };
//        state = bots[0].selectContract((WaitingForContractSelection) state);
//        while (state instanceof ContractInProgress cip) {
//            Player current = cip.activeContract().currentPlayer();
//            state = bots[current.ordinal()].play(cip);
//        }
//        return state;
//    }
//
//    // -------------------------------------------------------------------------
//    // 1. mapGameState — phase encoding
//    // -------------------------------------------------------------------------
//
//    @Test
//    void mapGameState_atStart_returnsWaitingSubtype() {
//        GameStateDTO dto = GameMapper.mapGameState(startingState(), VARIANT);
//
//        assertInstanceOf(GameStateDTO.WaitingForContractSelection.class, dto);
//        var waiting = (GameStateDTO.WaitingForContractSelection) dto;
//        assertEquals(Player.NORTH, waiting.currentDeclarer());
//        assertNotNull(waiting.cumulativeScores());
//    }
//
//    @Test
//    void mapGameState_contractInProgress_returnsContractSubtype() {
//        GameStateDTO dto = GameMapper.mapGameState(contractInProgress(), VARIANT);
//
//        assertInstanceOf(GameStateDTO.ContractInProgress.class, dto);
//        var inProgress = (GameStateDTO.ContractInProgress) dto;
//        assertEquals(Player.NORTH, inProgress.currentDeclarer());
//        assertNotNull(inProgress.currentPlayer());
//        assertNotNull(inProgress.tableState());
//    }
//
//    @Test
//    void mapGameState_contractInProgress_embedsTrickTableState() {
//        GameStateDTO dto = GameMapper.mapGameState(contractInProgress(), VARIANT);
//
//        var inProgress = (GameStateDTO.ContractInProgress) dto;
//        assertInstanceOf(TableStateDTO.Trick.class, inProgress.tableState());
//        var trick = (TableStateDTO.Trick) inProgress.tableState();
//        assertEquals(GameCatalog.HEARTS.id(), trick.contractId());
//    }
//
//    @Test
//    void mapGameState_reussiteContract_embedsTableauTableState() {
//        GameStateDTO dto = GameMapper.mapGameState(reussiteInProgress(), VARIANT);
//
//        var inProgress = (GameStateDTO.ContractInProgress) dto;
//        assertInstanceOf(TableStateDTO.Tableau.class, inProgress.tableState());
//    }
//
//    // -------------------------------------------------------------------------
//    // 2. mapPlayerState — per-player subtypes
//    // -------------------------------------------------------------------------
//
//    @Test
//    void mapPlayerState_currentDeclarer_returnsPickingContract() {
//        PlayerStateDTO dto = GameMapper.mapPlayerState(startingState(), Player.NORTH, VARIANT);
//
//        assertInstanceOf(PlayerStateDTO.PickingContract.class, dto);
//        var picking = (PlayerStateDTO.PickingContract) dto;
//        assertFalse(picking.hand().isEmpty());
//        assertTrue(picking.availableContracts().contains(GameCatalog.HEARTS.id()));
//    }
//
//    @Test
//    void mapPlayerState_nonDeclarer_returnsWaiting() {
//        PlayerStateDTO dto = GameMapper.mapPlayerState(startingState(), Player.EAST, VARIANT);
//
//        assertInstanceOf(PlayerStateDTO.Waiting.class, dto);
//        assertFalse(((PlayerStateDTO.Waiting) dto).hand().isEmpty());
//    }
//
//    @Test
//    void mapPlayerState_activePlayer_returnsPlaying() {
//        ContractInProgress state = contractInProgress();
//        Player current = state.activeContract().currentPlayer();
//        PlayerStateDTO dto = GameMapper.mapPlayerState(state, current, VARIANT);
//
//        assertInstanceOf(PlayerStateDTO.Playing.class, dto);
//        var playing = (PlayerStateDTO.Playing) dto;
//        assertFalse(playing.legalMoves().isEmpty());
//    }
//
//    @Test
//    void mapPlayerState_nonActivePlayer_returnsWaiting() {
//        ContractInProgress state = contractInProgress();
//        Player other = state.activeContract().currentPlayer().next();
//        PlayerStateDTO dto = GameMapper.mapPlayerState(state, other, VARIANT);
//
//        assertInstanceOf(PlayerStateDTO.Waiting.class, dto);
//    }
//
//    @Test
//    void mapPlayerState_gameOver_returnsGameOver() {
//        // Run a full game to reach GameOver
//        GameState state = GameState.newGame(SETTINGS, SEED);
//        FirstLegalMoveBot[] bots = {
//                new FirstLegalMoveBot(Player.NORTH), new FirstLegalMoveBot(Player.EAST),
//                new FirstLegalMoveBot(Player.SOUTH), new FirstLegalMoveBot(Player.WEST)
//        };
//        while (!(state instanceof GameOver)) {
//            state = switch (state) {
//                case WaitingForContractSelection w -> bots[w.currentDeclarer().ordinal()].selectContract(w);
//                case ContractInProgress cip -> bots[cip.activeContract().currentPlayer().ordinal()].play(cip);
//                case GameOver g -> g;
//            };
//        }
//        PlayerStateDTO dto = GameMapper.mapPlayerState(state, Player.NORTH, VARIANT);
//        assertInstanceOf(PlayerStateDTO.GameOver.class, dto);
//    }
//
//    // -------------------------------------------------------------------------
//    // 3. mapHistory
//    // -------------------------------------------------------------------------
//
//    @Test
//    void mapHistory_atStart_hasNoTurns() {
//        GameHistoryDTO dto = GameMapper.mapHistory(startingState(), VARIANT);
//        assertTrue(dto.turns().isEmpty());
//    }
//
//    @Test
//    void mapHistory_afterOneContract_hasOneDeclarerTurnWithKnownCatalogId() {
//        GameState state = oneContractFinished();
//        GameHistoryDTO dto = GameMapper.mapHistory(state, VARIANT);
//
//        assertEquals(1, dto.turns().size());
//        DeclarerTurnResultDTO turn = dto.turns().getFirst();
//        assertEquals(Player.NORTH, turn.declarer());
//        assertEquals(1, turn.playedContracts().size());
//
//        String contractId = turn.playedContracts().getFirst().contractId();
//        assertTrue(GameCatalog.all().stream()
//                .flatMap(v -> v.contracts().stream())
//                .anyMatch(cd -> cd.id().equals(contractId)),
//                "Expected a known catalog ID, got: " + contractId);
//    }
//
//    // -------------------------------------------------------------------------
//    // 4. TableStateDTO — trick card tracking
//    // -------------------------------------------------------------------------
//
//    @Test
//    void tableState_afterPlayingACard_showsCardInTrick() {
//        ContractInProgress state = contractInProgress();
//        Player current = state.activeContract().currentPlayer();
//        Card firstCard = state.activeContract().currentPlayerLegalCards().iterator().next();
//        GameState afterPlay = state.applyMove(new Move.PlayCard(current, firstCard));
//
//        var inProgress = (GameStateDTO.ContractInProgress) GameMapper.mapGameState(afterPlay, VARIANT);
//        var trick = (TableStateDTO.Trick) inProgress.tableState();
//
//        assertTrue(trick.cardsPlayed().containsKey(current));
//        assertEquals(firstCard, trick.cardsPlayed().get(current));
//    }
//}
