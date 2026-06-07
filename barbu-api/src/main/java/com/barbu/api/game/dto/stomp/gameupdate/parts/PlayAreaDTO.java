package com.barbu.api.game.dto.stomp.gameupdate.parts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.card.Suit;
import org.engine.contract.reussite.Tableau;
import org.engine.contract.trick.Trick;
import org.engine.game.Player;

import java.util.Map;

/**
 * Sealed DTO for the active play area. Embedded inside
 * {@link GameStateDTO.ContractInProgress}.
 *
 * <p>{@link TrickDTO} carries the cards played so far in the current trick.
 * {@link TableauDTO} carries the four suit piles for a Réussite round.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlayAreaDTO.TrickDTO.class, name = "TRICK"),
        @JsonSubTypes.Type(value = PlayAreaDTO.TableauDTO.class, name = "TABLEAU")
})
public sealed interface PlayAreaDTO permits PlayAreaDTO.TrickDTO, PlayAreaDTO.TableauDTO {

    record TrickDTO(Map<Player, Card> cardsPlayed, Map<Player, Integer> tricksTaken) implements PlayAreaDTO {}

    record TableauDTO(Map<Suit, Tableau.SuitPile> piles) implements PlayAreaDTO {}
}