package com.barbu.api.game.player.bot;

import org.engine.ai.Bot;
import org.engine.ai.HeuristicBot;
import org.engine.ai.RandomMoveBot;
import org.engine.ai.ismcts.IsmctsBot;
import org.engine.ai.mcts.MctsBot;


/**
 * AI difficulty level for a bot player.
 * <p>
 * Each constant implements {@link #createBot()} to produce the corresponding {@link org.engine.ai.Bot} instance.
 */
public enum BotType {
    EASY {
        @Override public Bot createBot() { return new RandomMoveBot(); }
    },
    MEDIUM {
        @Override public Bot createBot() {
            return new IsmctsBot(0.5, 50);
        }
    },
    HARD {
        @Override public Bot createBot() {
            return new IsmctsBot(0.5, 1000);
        }
    };

    public abstract Bot createBot();
}