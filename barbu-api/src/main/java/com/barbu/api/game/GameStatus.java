package com.barbu.api.game;

public enum GameStatus {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    FINISHED,
    ABANDONED // all human players left before the game finished
}