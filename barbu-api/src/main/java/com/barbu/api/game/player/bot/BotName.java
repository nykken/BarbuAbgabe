package com.barbu.api.game.player.bot;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Display names assigned to bot players.
 *
 * <p>Each bot seat is randomly assigned a name at game creation.
 */
public enum BotName {
    R2D2("R2-D2"),
    C3PO("C-3PO"),
    HAL("HAL 9000"),
    WALLE("WALL-E"),
    EVE("EVE"),
    DATA("Data"),
    BENDER("Bender"),
    MARVIN("Marvin"),
    BAYMAX("Baymax"),
    JOHNNY5("Johnny 5"),
    GORT("Gort"),
    JARVIS("JARVIS"),
    AVA("Ava"),
    BB8("BB-8"),
    BISHOP("Bishop"),
    ROBBY("Robby"),
    TWIKI("Twiki");

    private final String displayName;

    BotName(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}