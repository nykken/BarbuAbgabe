import { PopupType } from "../../apiHelper/Types.ts";
import P from "../ui/P.tsx";
import closeIcon from "../../assets/close-icon.svg";
import closeIconWhite from "../../assets/close-icon-white.svg";
import MenuContent from "./MenuContent.tsx";
import ScoresContent from "./ScoresContent.tsx";
import RulesContent from "./RulesContent.tsx";
import VariantManager from "../variants-and-contracts/VariantManager.tsx";

export default function PopupOverlay({
  type,
  onClose,
  fullSize = false,
}: {
  type: PopupType;
  onClose: () => void;
  fullSize?: boolean;
}) {
  const bgColor: Record<PopupType, string> = {
    [PopupType.Menu]: "bg-menu-settings",
    [PopupType.VariantRules]: "bg-menu-rules",
    [PopupType.GameRules]: "bg-gameboard",
    [PopupType.Scores]: "bg-menu-scores",
  };

  return (
    <div
      className={`border-text absolute top-1/2 left-1/2 z-50 flex size-full -translate-x-1/2 -translate-y-1/2 flex-col border lg:border-2 ${bgColor[type]} ${!fullSize && "sm:size-3/4 md:size-2/3 lg:size-3/5 xl:size-1/2"}`}
    >
      {/* popup title */}
      <div className="border-text flex w-full flex-1 items-center justify-between border-b pl-2 lg:border-b-2">
        <P>{type}</P>
        <button
          onClick={onClose}
          className="group flex aspect-square h-full cursor-pointer items-center justify-center border-l border-transparent p-1 hover:border-l-black hover:bg-red-700 lg:border-l-2 lg:hover:border-l-2"
        >
          <img
            src={closeIcon}
            alt="icon"
            className="block h-2.5 w-2.5 group-hover:hidden md:h-3 md:w-3 lg:h-4 lg:w-4"
          />
          <img
            src={closeIconWhite}
            alt="icon"
            className="hidden h-2.5 w-2.5 group-hover:block md:h-3 md:w-3 lg:h-4 lg:w-4"
          />
        </button>
      </div>

      {/* popup body */}
      <div className="flex min-h-0 flex-9 flex-col p-4">
        {type === PopupType.Menu && <MenuContent />}
        {type === PopupType.Scores && <ScoresContent />}
        {type === PopupType.VariantRules && <RulesContent />}
        {type === PopupType.GameRules && <VariantManager />}
      </div>
    </div>
  );
}
