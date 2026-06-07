package com.barbu.api.service;

import com.barbu.catalog.GameCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.engine.game.GameSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsJacksonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void standardSettings_roundTrip_equalsOriginal() throws Exception {
        GameSettings original = GameCatalog.STANDARD.toGameSettings();
        String json = MAPPER.writeValueAsString(original);
        assertEquals(original, MAPPER.readValue(json, GameSettings.class));
    }

    @Test
    void serializedJson_isValidJsonObject() throws Exception {
        String json = MAPPER.writeValueAsString(GameCatalog.STANDARD.toGameSettings());
        assertTrue(json.startsWith("{"));
    }

    @Test
    void contractList_preservesOrderAndEquality() throws Exception {
        GameSettings original = GameCatalog.STANDARD.toGameSettings();
        GameSettings restored = MAPPER.readValue(MAPPER.writeValueAsString(original), GameSettings.class);

        assertEquals(original.contracts().size(), restored.contracts().size());
        for (int i = 0; i < original.contracts().size(); i++) {
            assertEquals(original.contracts().get(i), restored.contracts().get(i),
                    "Contract at index " + i + " differs after round-trip");
        }
    }

    @Test
    void deckVariantAndRankingOrder_preserved() throws Exception {
        GameSettings original = GameCatalog.STANDARD.toGameSettings();
        GameSettings restored = MAPPER.readValue(MAPPER.writeValueAsString(original), GameSettings.class);

        assertEquals(original.deckVariant(), restored.deckVariant());
        assertEquals(original.rankingOrder(), restored.rankingOrder());
    }
}
