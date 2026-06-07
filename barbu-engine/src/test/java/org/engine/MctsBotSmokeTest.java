package org.engine;

import org.engine.ai.Bot;
import org.engine.ai.RandomMoveBot;
import org.engine.ai.mcts.MctsBot;
import org.engine.game.GameSettings;
import org.engine.game.Player;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.GameOver;
import org.engine.game.state.GameState;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.engine.helpers.TestSettings.extended;
import static org.engine.helpers.TestSettings.standard;
import static org.junit.jupiter.api.Assertions.*;

class MctsBotSmokeTest {

    private static Map<Player, Bot> mixedBots() {
        Bot mcts = new MctsBot(new RandomMoveBot(), Math.sqrt(2), 2, 10);
        return Map.of(
                Player.NORTH, mcts,
                Player.EAST,  new RandomMoveBot(),
                Player.SOUTH, new RandomMoveBot(),
                Player.WEST,  new RandomMoveBot()
        );
    }

    @Test
    void mctsAndRandomBotsCompleteAFullGameStandard() {
        GameSettings settings = standard();
        Map<Player, Bot> bots = mixedBots();
        GameState state = GameState.newGame(settings, 42L);

        while (state instanceof ActiveGameState active) {
            state = active.applyMove(bots.get(active.currentPlayer()).chooseMove(active));
        }

        assertInstanceOf(GameOver.class, state);
        for (Player p : Player.values()) {
            assertEquals(settings.contracts().size(),
                         state.history().finishedContractsForDeclarer(p).size());
            assertTrue(state.remainingContracts(p).isEmpty());
        }
    }

    @Test
    void mctsAndRandomBotsCompleteAFullGameExtended() {
        GameSettings settings = extended();
        Map<Player, Bot> bots = mixedBots();
        GameState state = GameState.newGame(settings, 42L);

        while (state instanceof ActiveGameState active) {
            state = active.applyMove(bots.get(active.currentPlayer()).chooseMove(active));
        }

        assertInstanceOf(GameOver.class, state);
        for (Player p : Player.values()) {
            assertEquals(settings.contracts().size(),
                         state.history().finishedContractsForDeclarer(p).size());
            assertTrue(state.remainingContracts(p).isEmpty());
        }
    }

    @Test
    void mctsAndRandomBotsCompleteAFullGameRejection() {
        GameSettings settings = standard();
        Bot mcts = new MctsBot(new RandomMoveBot(), Math.sqrt(2), 2, 10);
        Map<Player, Bot> bots = Map.of(
                Player.NORTH, mcts,
                Player.EAST,  new RandomMoveBot(),
                Player.SOUTH, new RandomMoveBot(),
                Player.WEST,  new RandomMoveBot()
        );
        GameState state = GameState.newGame(settings, 42L);

        while (state instanceof ActiveGameState active) {
            state = active.applyMove(bots.get(active.currentPlayer()).chooseMove(active));
        }

        assertInstanceOf(GameOver.class, state);
        for (Player p : Player.values()) {
            assertEquals(settings.contracts().size(),
                         state.history().finishedContractsForDeclarer(p).size());
            assertTrue(state.remainingContracts(p).isEmpty());
        }
    }
}