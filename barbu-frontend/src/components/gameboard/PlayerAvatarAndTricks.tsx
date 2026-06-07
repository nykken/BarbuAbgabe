import type { Position } from "../../apiHelper/Types.ts";
import avatarIcon from "../../assets/avatar.svg";
import botIcon from "../../assets/bot-icon.svg";
import trickIcon from "../../assets/trick.svg";
import { playerColor } from "../../apiHelper/playerColorRecord.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";

export default function PlayerAvatarAndTricks({
  position,
}: {
  position: Position;
}) {
  const playerBackendPosition = GameStateProvider((state) =>
    state.getPosByUiPos(position),
  );
  const isActive = GameStateProvider((state) =>
    state.getIsActiveByUiPos(position),
  );
  const trickCount = GameStateProvider((state) =>
    state.getTrickCountByUiPos(position),
  );
  const playerType = GameStateProvider((state) =>
    state.getPlayerTypeByUiPos(position),
  );

  if (!playerBackendPosition) return;

  return (
    <div
      className={`m-0 flex items-end p-0 lg:gap-1 lg:gap-1.5 ${position == "right" && "flex-row-reverse"} ${position === "bottom" && "flex-col-reverse items-start"} ${position === "left" || position === "right" ? "w-full gap-0.5" : "h-full gap-1"} `}
    >
      <div>
        <div
          className={`border-text ${playerColor[playerBackendPosition]} relative flex aspect-square h-5 items-center justify-center overflow-hidden rounded-full border pt-0.5 sm:h-6 lg:h-7 xl:h-8`}
        >
          {/* blink overlay */}
          {isActive && (
            <div className="animate-inner-pulse absolute inset-0 z-10 rounded-full bg-black" />
          )}
          <img
            src={playerType === "BOT" ? botIcon : avatarIcon}
            alt="avatar icon"
            className="object-contain p-0.5"
          />
        </div>
      </div>

      <div className={`relative`}>
        {trickCount > 0 && (
          <div>
            <div
              className={`absolute top-0 right-0 z-8 flex h-full w-3.5 items-center justify-center sm:w-4 lg:w-5 xl:w-6`}
            >
              <p className="text-[8px] sm:text-[10px] lg:text-[12px]">
                {trickCount}
              </p>
            </div>
            <img
              src={trickIcon}
              alt="trick icon"
              className={`h-5 sm:h-6 lg:h-7 xl:h-8`}
            />
          </div>
        )}
      </div>
    </div>
  );
}
