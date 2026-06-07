package com.barbu.api.game.dto.stomp.gameupdate.parts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.game.Player;

/**
 * Sealed DTO mirroring the engine's {@code Move} types. Used for:
 * <ul>
 *   <li>Incoming client moves over WebSocket (compact: contract referenced by catalog ID)</li>
 *   <li>Outgoing move broadcast after each turn so the frontend can render animations</li>
 * </ul>
 * Use {@code GameMapper.toMove(MoveDTO, GameVariant)} to convert an incoming DTO to an engine Move.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OutgoingMoveDTO.SelectContract.class, name = "SELECT_CONTRACT"),
        @JsonSubTypes.Type(value = OutgoingMoveDTO.PlayCard.class, name = "PLAY_CARD"),
        @JsonSubTypes.Type(value = OutgoingMoveDTO.Pass.class, name = "PASS")
})
public sealed interface OutgoingMoveDTO permits OutgoingMoveDTO.SelectContract, OutgoingMoveDTO.PlayCard, OutgoingMoveDTO.Pass {

    record SelectContract(Player declarer) implements OutgoingMoveDTO {}
    record PlayCard(Player player, Card card) implements OutgoingMoveDTO {}
    record Pass(Player player) implements OutgoingMoveDTO {}
}