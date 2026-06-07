package com.barbu.api.game.dto.stomp.gameupdate.parts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.card.Suit;
import org.engine.contract.reussite.Tableau;
import org.engine.game.Player;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CompletedPlayAreaDTO.CompletedTrickDTO.class, name = "TRICK"),
        @JsonSubTypes.Type(value = CompletedPlayAreaDTO.CompletedTableauDTO.class, name = "TABLEAU"),
})
public sealed interface CompletedPlayAreaDTO permits CompletedPlayAreaDTO.CompletedTrickDTO, CompletedPlayAreaDTO.CompletedTableauDTO {
    record CompletedTableauDTO(Map<Suit, Tableau.SuitPile> piles, Player lastPlayer) implements CompletedPlayAreaDTO {}

    record CompletedTrickDTO(Map<Player, Card> cards, Player winner) implements CompletedPlayAreaDTO {}
}