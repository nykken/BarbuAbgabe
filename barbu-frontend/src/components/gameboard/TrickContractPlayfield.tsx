import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import cardImages from "../../apiHelper/CardRecord.tsx";
import type { Card, Player } from "../../apiHelper/GameTypes.ts";
import type { Position } from "../../apiHelper/Types.ts";
import { useEffect, useMemo, useState } from "react";
import TrickCardImages from "./TrickCardImage.tsx";

const SHOW_MS = 1200;
const TOTAL_MS = 1800;

export default function TrickContractPlayfield() {
  const tableState = GameStateProvider((state) =>
    state.publicState?.gameState.type === "CONTRACT_IN_PROGRESS" &&
    state.publicState.gameState.tableState.type === "TRICK"
      ? state.publicState.gameState.tableState
      : null,
  );

  const pendingPlayAreaDisplay = GameStateProvider(
    (state) => state.pendingPlayAreaDisplay,
  );
  const pendingTrickDisplay =
    pendingPlayAreaDisplay?.type === "TRICK" ? pendingPlayAreaDisplay : null;

  const BE_ORDER: Player[] = ["NORTH", "EAST", "SOUTH", "WEST"];
  const UI_ORDER: Position[] = ["bottom", "left", "top", "right"];

  const myPlayerPosition = GameStateProvider((state) =>
    state.getPosByUiPos("bottom"),
  );
  const myPlayerIndex = myPlayerPosition
    ? BE_ORDER.indexOf(myPlayerPosition)
    : 0;

  const playerToPosition = useMemo(() => {
    return BE_ORDER.reduce(
      (positionMap, player, index) => {
        const relativeIndex = (index - myPlayerIndex + 4) % 4;
        positionMap[player] = UI_ORDER[relativeIndex];
        return positionMap;
      },
      {} as Record<Player, Position>,
    );
  }, [myPlayerIndex]);

  const [visualCards, setVisualCards] = useState<Record<Player, Card>>(
    {} as Record<Player, Card>,
  );
  const [winnerUiPos, setWinnerUiPos] = useState<Position | null>(null);

  // Effect 1: update which cards are shown
  useEffect(() => {
    if (pendingTrickDisplay) {
      setVisualCards(pendingTrickDisplay.cards);
    } else if (tableState) {
      setVisualCards(tableState.cardsPlayed);
    } else {
      setVisualCards({} as Record<Player, Card>);
    }
  }, [tableState, pendingTrickDisplay]);

  // Effect 2: two-phase animation — only restarts when pendingTrickDisplay itself changes
  useEffect(() => {
    if (!pendingTrickDisplay) return;
    setWinnerUiPos(null);

    const phase2 = setTimeout(() => {
      setWinnerUiPos(playerToPosition[pendingTrickDisplay.winner]);
    }, SHOW_MS);

    const flush = setTimeout(() => {
      setWinnerUiPos(null);
      GameStateProvider.getState().flushPendingUpdates();
    }, TOTAL_MS);

    return () => {
      clearTimeout(phase2);
      clearTimeout(flush);
    };
  }, [pendingTrickDisplay]);

  if (!pendingTrickDisplay && !tableState) return null;
  if (!myPlayerPosition) return null;

  const playedCards = Object.entries(visualCards).map(([player, card]) => ({
    player: player as Player,
    card: card as Card,
    position: playerToPosition[player as Player],
  }));

  const zBase = 10;

  return (
    <div className="relative flex size-full items-center justify-center">
      {playedCards.map((entry, index) => {
        const key = `${entry.card.suit}${entry.card.rank}`;

        return (
          <TrickCardImages
            key={`${entry.player}-${key}`}
            position={entry.position as Position}
            src={cardImages[key]}
            zIndex={zBase + index}
            winnerPosition={winnerUiPos}
          />
        );
      })}
    </div>
  );
}
