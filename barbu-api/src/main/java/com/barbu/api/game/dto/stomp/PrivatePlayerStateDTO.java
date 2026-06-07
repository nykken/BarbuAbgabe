package com.barbu.api.game.dto.stomp;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.engine.card.Card;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrivatePlayerStateDTO(Set<Card> hand, Set<Card> legalMoves) {}