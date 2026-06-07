package org.engine.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Card;
import org.engine.contract.Contract;

/**
 * A move a player can make during a game.
 *
 * <p>Three concrete variants exist as nested records:
 * {@link SelectContract}, {@link PlayCard}, and {@link Pass}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Move.SelectContract.class, name = "SELECT_CONTRACT"),
        @JsonSubTypes.Type(value = Move.PlayCard.class,       name = "PLAY_CARD"),
        @JsonSubTypes.Type(value = Move.Pass.class,           name = "PASS")
})
public interface Move {
    /** Declares a contract for the current round. */
    record SelectContract(Player declarer, Contract contract) implements Move {}

    /** Plays a card from the player's hand. */
    record PlayCard(Player player, Card card) implements Move {}

    /** Passes */
    record Pass(Player player) implements Move {}
}