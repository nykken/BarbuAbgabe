import cardBack from "../../assets/card-back-small.svg";
import cardBackRotated from "../../assets/card-back-small-rotated.svg";
import PlayerAvatarAndTricks from "./PlayerAvatarAndTricks.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";

export default function CardDeckOpponent({
  position,
}: {
  position: "bottom" | "right" | "top" | "left";
}) {
  const isVertical = position === "left" || position === "right";
  const cardCount = GameStateProvider((s) => s.getCardCountByUiPos(position));

  return (
    <div
      className={`relative flex gap-1 lg:gap-2 ${isVertical ? "flex-col-reverse" : "h-full"}`}
    >
      {/* Debug Info */}
      {/*<div className="pointer-events-none absolute top-0 left-0 z-50 bg-black/50 p-0.5 text-[8px] text-white">
        UI: {position} | BE: {playerBackendPosition}
      </div>*/}

      {!isVertical && <div className="w-10"></div>}

      <div
        className={`flex ${isVertical ? "flex-col" : "-mt-5 h-[200%] sm:-mt-6 md:-mt-7 lg:-mt-10 xl:-mt-12"}`}
      >
        {Array.from({ length: cardCount }).map((_, index) => (
          <img
            key={index}
            src={isVertical ? cardBackRotated : cardBack}
            alt="game card"
            className={`object-cover ${
              isVertical
                ? "-mb-5.5 w-screen last:mb-0 sm:-mb-6 md:-mb-7 lg:-mb-10 xl:-mb-12"
                : "-mr-5.5 h-full last:mr-0 sm:-mr-6 md:-mr-7 lg:-mr-10 xl:-mr-12"
            }`}
          />
        ))}
      </div>

      <PlayerAvatarAndTricks position={position} />
    </div>
  );
}
