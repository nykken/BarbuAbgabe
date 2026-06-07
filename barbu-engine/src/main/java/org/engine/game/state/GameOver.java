package org.engine.game.state;

import org.engine.game.GameSettings;
import org.engine.game.History;

/**
 * Game state after all declarers have completed all their contracts.
 */
public final class GameOver extends GameState {
    GameOver(GameSettings settings, History history) {
        super(settings, history);
    }
}