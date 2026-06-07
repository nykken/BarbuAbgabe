import { type ReactNode } from "react";
import { PopupType } from "../../apiHelper/Types.ts";
import P from "../ui/P.tsx";
import menuIcon from "../../assets/menu-icon.svg";
import helpIcon from "../../assets/help-icon.svg";
import trophyIcon from "../../assets/trophy-icon.svg";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";

export default function MenuBar() {
  const currentContractRound = GameStateProvider((state) =>
    state.getContractRound(),
  );
  const maxContractRounds = GameStateProvider((state) =>
    state.getMaxContractRounds(),
  );
  const currentContractName = GameStateProvider((state) =>
    state.getContractName(),
  );
  const currentDeclarerRound = GameStateProvider((state) =>
    state.getDeclarerRound(),
  );
  const setPopup = GameStateProvider((state) => state.setPopup);

  return (
    <div className="z-20 flex h-5 justify-between sm:h-6 md:h-7 lg:h-10 xl:h-12">
      {/* left menu */}
      <div className="flex gap-1 sm:gap-2 lg:gap-3">
        <MenuPopupButton
          openPopupFunction={setPopup}
          popupType={PopupType.Menu}
          rounded={true}
          hoverColor={"hover:bg-menu-settings"}
        >
          <img src={menuIcon} alt="icon" className="object-contain" />
        </MenuPopupButton>
        <MenuRectElement>
          <P>
            {currentContractRound}/{maxContractRounds}
          </P>
        </MenuRectElement>
        <MenuPopupButton
          openPopupFunction={setPopup}
          popupType={PopupType.VariantRules}
          hoverColor={"hover:bg-menu-rules"}
        >
          <P>{currentContractName}</P>
          <img
            src={helpIcon}
            alt="icon"
            className="size-4 object-contain sm:size-5 lg:size-7"
          />
        </MenuPopupButton>
      </div>
      {/* right menu */}
      <div className="flex gap-1 sm:gap-2 lg:gap-3">
        <MenuPopupButton
          openPopupFunction={setPopup}
          popupType={PopupType.Scores}
          rounded={true}
          hoverColor={"hover:bg-menu-scores"}
        >
          <img src={trophyIcon} alt="icon" className="object-contain" />
        </MenuPopupButton>
        <MenuRectElement>
          <P>Round</P>
          <P>{currentDeclarerRound}/4</P>
        </MenuRectElement>
      </div>
    </div>
  );
}

function MenuPopupButton({
  children,
  openPopupFunction,
  popupType,
  rounded = false,
  hoverColor,
}: {
  children?: ReactNode;
  openPopupFunction: (type: PopupType | null) => void;
  popupType: PopupType;
  rounded?: boolean;
  hoverColor: string;
}) {
  return (
    <button
      className={`border-text flex h-full items-center justify-center gap-2 border p-0.5 lg:border-2 lg:p-1 xl:p-2 ${
        rounded
          ? "aspect-square rounded-full"
          : "w-fit rounded-sm px-1 sm:rounded-md lg:rounded-lg lg:px-2 xl:px-3"
      } ${hoverColor} cursor-pointer`}
      onClick={() => {
        openPopupFunction(popupType);
      }}
    >
      {children}
    </button>
  );
}
function MenuRectElement({ children }: { children: ReactNode }) {
  return (
    <div className="border-text flex h-full items-center justify-between gap-2 rounded-sm border px-1 sm:rounded-md lg:rounded-lg lg:border-2 lg:px-2 xl:px-3">
      {children}
    </div>
  );
}
