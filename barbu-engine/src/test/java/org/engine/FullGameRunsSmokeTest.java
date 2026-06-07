package org.engine;

import org.engine.ai.Bot;
import org.engine.ai.HeuristicBot;
import org.engine.ai.RandomMoveBot;
import org.engine.game.*;
import org.engine.game.state.ActiveGameState;
import org.engine.game.state.GameOver;
import org.engine.game.state.GameState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.engine.helpers.TestSettings.extended;
import static org.engine.helpers.TestSettings.standard;
import static org.junit.jupiter.api.Assertions.*;

class FullGameRunsSmokeTest {

    static Stream<Bot> bots() {
        return Stream.of(new HeuristicBot(), new RandomMoveBot());
    }

    @ParameterizedTest(name = "standard – {0}")
    @MethodSource("bots")
    void fourBotsCompleteAFullGameStandard(Bot bot) {
        GameSettings settings = standard();
        GameState state = GameState.newGame(settings, 42L);

        while (state instanceof ActiveGameState active) {
            state = active.applyMove(bot.chooseMove(active));
        }

        assertInstanceOf(GameOver.class, state);

        for (Player p : Player.values()) {
            assertEquals(settings.contracts().size(), state.history().finishedContractsForDeclarer(p).size());
        }

        for (Player p : Player.values()) {
            assertTrue(state.remainingContracts(p).isEmpty());
        }
    }

    @ParameterizedTest(name = "extended – {0}")
    @MethodSource("bots")
    void fourBotsCompleteAFullGameExtended(Bot bot) {
        GameSettings settings = extended();
        GameState state = GameState.newGame(settings, 42L);

        while (state instanceof ActiveGameState active) {
            state = active.applyMove(bot.chooseMove(active));
        }

        assertInstanceOf(GameOver.class, state);

        for (Player p : Player.values()) {
            assertEquals(settings.contracts().size(), state.history().finishedContractsForDeclarer(p).size());
        }

        for (Player p : Player.values()) {
            assertTrue(state.remainingContracts(p).isEmpty());
        }
    }
}