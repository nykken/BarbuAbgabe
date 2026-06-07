package org.engine.game.state;

import org.engine.card.*;
import org.engine.contract.*;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.game.*;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.engine.helpers.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class ReussiteContractTest {

    private static final int FIRST_PLACE_POINTS  = -100;
    private static final int SECOND_PLACE_POINTS = -60;

    private static final Player NORTH = Player.NORTH;
    private static final Player EAST  = Player.EAST;
    private static final Player SOUTH = Player.SOUTH;
    private static final Player WEST  = Player.WEST;

    /** Builds a minimal ContractInProgress with a Réussite contract already active. */
    private static ContractInProgress startReussite(
            Map<Player, Hand> hands, @Nullable Rank startingRank,
            Player declarer, @Nullable Rank replayAfterRank) {
        ReussiteContract definition = new ReussiteContract(
                startingRank, List.of(FIRST_PLACE_POINTS, SECOND_PLACE_POINTS), replayAfterRank);
        GameSettings settings = new GameSettings(
                List.of(definition), Deck.Variant.FROM_SEVEN, GameSettings.RankingOrder.LOWEST_SCORE_WINS);
        ContractState active = definition.start(declarer, hands);
        return new ContractInProgress(settings, History.create(), declarer, active, 0L);
    }

    @Test
    void testAceGrantsExtraTurn() {
        ContractInProgress state = startReussite(
                handsOf(h("AH", "KH", "JH"), h("7S"), h("7D"), h("7C")),
                null, NORTH, Rank.ACE);

        GameState next = state.applyMove(new Move.PlayCard(NORTH, c("AH")));
        assertInstanceOf(ContractInProgress.class, next);
        assertEquals(NORTH, ((ContractInProgress) next).activeContract().currentPlayer(),
                "Turn should stay on North after Ace");

        GameState next2 = ((ActiveGameState) next).applyMove(new Move.PlayCard(NORTH, c("KH")));
        assertInstanceOf(ContractInProgress.class, next2);
        assertEquals(EAST, ((ContractInProgress) next2).activeContract().currentPlayer(),
                "Turn should advance after non-Ace");
    }

    @Test
    void testSkipFinishedPlayers() {
        ContractInProgress state = startReussite(
                handsOf(h("JH"), h("AC"), h("7D"), h("7C", "JC")),
                Rank.JACK, NORTH, Rank.ACE);

        GameState s = state.applyMove(new Move.PlayCard(NORTH, c("JH"))); // North finishes
        s = ((ActiveGameState) s).applyMove(new Move.Pass(EAST));
        s = ((ActiveGameState) s).applyMove(new Move.Pass(SOUTH));
        s = ((ActiveGameState) s).applyMove(new Move.PlayCard(WEST, c("JC")));

        // North is finished, so next player should be East
        assertInstanceOf(ContractInProgress.class, s);
        assertEquals(EAST, ((ContractInProgress) s).activeContract().currentPlayer(),
                "Turn should skip finished North");
    }

    @Test
    void testCannotPassWithLegalMoves() {
        ContractInProgress state = startReussite(
                handsOf(h("8H", "JC"), h("7H"), h("7D"), h("7C")),
                Rank.JACK, NORTH, Rank.ACE);

        assertThrows(IllegalMoveException.class,
                () -> state.applyMove(new Move.Pass(NORTH)));
    }

    @Test
    void testScoringWhenRoundEnds() {
        ContractInProgress state = startReussite(
                handsOf(h("JC"), h("JH"), h("9D"), h("9C")),
                Rank.JACK, NORTH, Rank.ACE);

        GameState s = state.applyMove(new Move.PlayCard(NORTH, c("JC"))); // North 1st
        assertFalse(s instanceof GameOver);

        s = ((ActiveGameState) s).applyMove(new Move.PlayCard(EAST, c("JH")));  // East 2nd

        // Contract finished — scores accumulated
        Map<Player, Integer> scores = s.cumulativeScores();
        assertEquals(FIRST_PLACE_POINTS,  scores.get(NORTH));
        assertEquals(SECOND_PLACE_POINTS, scores.get(EAST));
        assertEquals(0, scores.get(SOUTH));
        assertEquals(0, scores.get(WEST));
    }

    @Test
    void testNoReplayWhenReplayRankIsNull() {
        ContractInProgress state = startReussite(
                handsOf(h("AH", "KH"), h("7S"), h("7D"), h("7C")),
                null, NORTH, null);
        GameState next = state.applyMove(new Move.PlayCard(NORTH, c("AH")));
        assertInstanceOf(ContractInProgress.class, next);
        assertEquals(EAST, ((ContractInProgress) next).activeContract().currentPlayer(),
                "Turn should advance after Ace when replay is disabled");
    }

    @Test
    void testNoReplayWhenPlayerFinishesOnReplayRank() {
        ContractInProgress state = startReussite(
                handsOf(h("AH"), h("7S"), h("7D"), h("7C")),
                null, NORTH, Rank.ACE);

        GameState next = state.applyMove(new Move.PlayCard(NORTH, c("AH")));
        assertInstanceOf(ContractInProgress.class, next);
        assertEquals(EAST, ((ContractInProgress) next).activeContract().currentPlayer(),
                "Finished player should not get a replay turn");
    }
}