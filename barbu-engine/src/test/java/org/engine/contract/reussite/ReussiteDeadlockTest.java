package org.engine.contract.reussite;

import org.engine.card.Card;
import org.engine.card.Hand;
import org.engine.card.Rank;
import org.engine.card.Suit;
import org.engine.game.Move;
import org.engine.game.Player;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the defensive guard in {@link ReussiteState#applyPass}: if no active
 * player has a card legal on the current tableau, the engine throws rather than
 * letting four players take turns passing forever.
 *
 * <p>This situation cannot arise in a real Réussite game (the start-rank ladder
 * is always playable), but the engine has no way to prove that, so the guard
 * exists to surface any future regression as a hard failure instead of an
 * infinite loop.
 *
 * <p>Lives in the {@code org.engine.contract.reussite} package so it can reach
 * the package-private {@link ReussiteState} constructor.
 */
class ReussiteDeadlockTest {

    @Test
    void applyPass_throwsWhenNoActivePlayerHasALegalCard() {
        ReussiteContract contract = new ReussiteContract(
                Rank.JACK, List.of(-100, -60), Rank.ACE);

        Tableau emptyTableauWithJackStart = new Tableau(Rank.JACK);

        Map<Player, Hand> hands = new EnumMap<>(Player.class);
        hands.put(Player.NORTH, new Hand(List.of(new Card(Suit.CLUBS, Rank.ACE))));
        hands.put(Player.EAST,  new Hand(List.of(new Card(Suit.DIAMONDS, Rank.ACE))));
        hands.put(Player.SOUTH, new Hand(List.of(new Card(Suit.HEARTS, Rank.ACE))));
        hands.put(Player.WEST,  new Hand(List.of(new Card(Suit.SPADES, Rank.ACE))));

        ReussiteState deadlocked = new ReussiteState(
                contract, Player.NORTH, hands, emptyTableauWithJackStart, List.of());

        assertThrows(IllegalStateException.class,
                () -> deadlocked.applyMove(new Move.Pass(Player.NORTH)));
    }
}