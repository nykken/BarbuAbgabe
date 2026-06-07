package org.engine.contract;

import org.engine.game.Move;

/**
 * Thrown when a {@link Move} is applied to a {@link ContractState} in which it is not legal.
 */
public class IllegalMoveException extends RuntimeException {
    public IllegalMoveException(String message) {
        super(message);
    }
}