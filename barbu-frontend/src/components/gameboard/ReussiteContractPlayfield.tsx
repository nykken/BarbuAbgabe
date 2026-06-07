import cardImages from "../../apiHelper/CardRecord.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import { motion } from "motion/react";
import type { Suit } from "../../apiHelper/GameTypes.ts";
import { useEffect } from "react";

const TABLEAU_SHOW_MS = 2000;
const ALL_SUITS = ["C", "D", "S", "H"];
const rankOrder = [
  "A",
  "K",
  "Q",
  "J",
  "10",
  "9",
  "8",
  "7",
  "6",
  "5",
  "4",
  "3",
  "2",
];
const CARD_SIZED = "h-18 sm:h-22 md:h-26 lg:h-32 xl:h-38";
const GAPS = {
  first: "mt-0",
  big: "mt-[-3.5rem] sm:mt-[-4.3rem] md:mt-[-5rem] lg:mt-[-6.2rem] xl:mt-[-7.2rem]",
  small:
    "mt-[-4.2rem] sm:mt-[-5.15rem] md:mt-[-6rem] lg:mt-[-7.4rem] xl:mt-[-8.8rem]",
};

export default function ReussiteContractPlayfield() {
  const pendingPlayAreaDisplay = GameStateProvider(
    (state) => state.pendingPlayAreaDisplay,
  );

  const tableState = GameStateProvider((state) => {
    // pending table state
    const pending = state.pendingPlayAreaDisplay;
    if (pending?.type === "TABLEAU") return pending;
    // active table state
    const active = state.publicState?.gameState;
    if (
      active?.type === "CONTRACT_IN_PROGRESS" &&
      active.tableState.type === "TABLEAU"
    ) {
      return active.tableState;
    }
    return null;
  });

  useEffect(() => {
    if (pendingPlayAreaDisplay?.type !== "TABLEAU") return;
    const flush = setTimeout(() => {
      GameStateProvider.getState().flushPendingUpdates();
    }, TABLEAU_SHOW_MS);
    return () => clearTimeout(flush);
  }, [pendingPlayAreaDisplay]);

  if (!tableState) return null;

  return (
    <div className="flex size-full items-center justify-around px-5 md:px-10 lg:px-14 xl:px-24">
      {ALL_SUITS.map((suit) => {
        const activePile = tableState.piles[suit as Suit];

        // slice() extracts cards from high rank to low rank based on the new order
        const cardsInPile = activePile
          ? (() => {
              const indexLow = rankOrder.indexOf(activePile.low);
              const indexHigh = rankOrder.indexOf(activePile.high);

              const start = Math.min(indexLow, indexHigh);
              const end = Math.max(indexLow, indexHigh);

              const slice = rankOrder.slice(start, end + 1);

              return slice.map((rank, index) => {
                const isTopOrBottom = index === 0 || index === slice.length - 1;
                const cardId = `${suit}${rank}`;

                return {
                  id: cardId,
                  // for top and bottom card we use the real image
                  // otherwise renders card without rank to be visually more pleasing
                  src: isTopOrBottom
                    ? cardImages[cardId]
                    : cardImages[`${suit}White`],
                };
              });
            })()
          : [];

        return (
          <div
            key={suit}
            className="flex h-full flex-col items-center justify-center"
          >
            {cardsInPile.length === 0 ? (
              /* placeholder img */
              <img
                src={cardImages[`${suit}Placeholder`]}
                alt="${suit} placeholder"
                className={`${CARD_SIZED}`}
              />
            ) : (
              /* card pile */
              cardsInPile.map((card, index) => (
                <motion.img
                  key={card.id}
                  src={card.src}
                  alt={card.id}
                  style={{ zIndex: index + 1 }}
                  initial={{ opacity: 0, scale: 0.5 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ type: "spring", stiffness: 300, damping: 25 }}
                  className={`${CARD_SIZED} ${
                    index === 0
                      ? GAPS.first // top card
                      : index === 1
                        ? GAPS.big // bigger gap between first and second card
                        : GAPS.small // tight stack for everything else
                  }`}
                />
              ))
            )}
          </div>
        );
      })}
    </div>
  );
}
