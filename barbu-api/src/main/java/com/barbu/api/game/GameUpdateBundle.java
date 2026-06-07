package com.barbu.api.game;

import com.barbu.api.game.dto.stomp.PrivatePlayerStateDTO;
import com.barbu.api.game.dto.stomp.gameupdate.PublicGameUpdateDTO;

import java.util.Map;

/** Game state snapshot to broadcast: public game update + per-human private state. */
public record GameUpdateBundle(PublicGameUpdateDTO publicUpdate, Map<String, PrivatePlayerStateDTO> privateUpdates) {}