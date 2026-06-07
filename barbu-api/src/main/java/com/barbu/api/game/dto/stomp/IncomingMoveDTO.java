package com.barbu.api.game.dto.stomp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;

/**
 * Incoming move from the client. No player field, position is derived in the backend
 * from the authenticated user's seat in {@code game_players}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = IncomingMoveDTO.SelectContract.class, name = "SELECT_CONTRACT"),
        @JsonSubTypes.Type(value = IncomingMoveDTO.PlayCard.class, name = "PLAY_CARD"),
        @JsonSubTypes.Type(value = IncomingMoveDTO.Pass.class, name = "PASS")
})
public sealed interface IncomingMoveDTO
        permits IncomingMoveDTO.SelectContract, IncomingMoveDTO.PlayCard, IncomingMoveDTO.Pass {

    record SelectContract(String contractId) implements IncomingMoveDTO {}
    record PlayCard(Card card)               implements IncomingMoveDTO {}
    record Pass()                            implements IncomingMoveDTO {}
}