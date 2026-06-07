package org.engine.contract;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Hand;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.game.Player;

import java.util.Map;

/**
 * Configuration for a Barbu contract.
 *
 * <p>One instance exists per contract type in {@link org.engine.game.GameSettings}.
 * Call {@link #start} to produce a {@link ContractState} when play begins.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReussiteContract.class, name = "REUSSITE"),
        @JsonSubTypes.Type(value = TrickTakingContract.class, name = "TRICK_TAKING")
})
public interface Contract {
    /**
     * Creates the in-progress state for this contract
     *
     * @param declarer the player who declared this contract
     * @param hands    the dealt hands, one per player
     * @return the initial {@link ContractState} ready for the first move
     */
    ContractState start(Player declarer, Map<Player, Hand> hands);
}