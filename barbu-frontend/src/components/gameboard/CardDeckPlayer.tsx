import PlayerAvatarAndTricks from "./PlayerAvatarAndTricks.tsx";
import ClickableCard from "./ClickableCard.tsx";
import { useState } from "react";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import type { Card } from "../../apiHelper/GameTypes.ts";
import SubmitButton from "../ui/SubmitButton.tsx";
import { useShallow } from "zustand/react/shallow";

export default function CardDeckPlayer() {
  const [selectedCard, setSelectedCard] = useState<string | null>(null);

  const sortedHand = GameStateProvider(
    useShallow((state) => state.getSortedHand()),
  );
  const isActive = GameStateProvider((state) =>
    state.getIsActiveByUiPos("bottom"),
  );
  const canPass = GameStateProvider((state) => state.checkCanPass());
  const playCard = GameStateProvider((state) => state.playCard);
  const pass = GameStateProvider((state) => state.pass);
  const legalMoves = GameStateProvider(
    useShallow((state) => state.privateState?.legalMoves ?? []),
  );

  const isCardPlayable = (card: Card) =>
    legalMoves.some(
      (cardInLegalMoves) =>
        cardInLegalMoves.suit === card.suit &&
        cardInLegalMoves.rank === card.rank,
    );

  const handleCardClick = (card: Card) => {
    const cardId = card.suit + card.rank;
    if (!isActive) return;

    if (selectedCard === cardId) {
      playCard(card);
      setSelectedCard(null);
    } else {
      setSelectedCard(cardId);
    }
  };

  return (
    <div className="flex h-full justify-center gap-1 lg:gap-2">
      <div className="flex flex-1 items-start justify-end">
        {canPass && <SubmitButton text="pass" onClick={pass} />}
      </div>
      <div className="flex w-fit justify-center">
        <div className="flex h-full w-fit">
          {sortedHand.map((card) => {
            const cardId = card.suit + card.rank;

            const playable = isCardPlayable(card);

            const showAsPlayable = !isActive || playable;
            const canClick = isActive && playable;

            return (
              <ClickableCard
                key={cardId}
                suit={card.suit}
                rank={card.rank}
                isSelected={selectedCard === cardId}
                onClick={canClick ? () => handleCardClick(card) : undefined}
                isPlayable={showAsPlayable}
              />
            );
          })}
        </div>
      </div>

      <div className="flex flex-1 items-end">
        <PlayerAvatarAndTricks position="bottom" />
      </div>
    </div>
  );
}
