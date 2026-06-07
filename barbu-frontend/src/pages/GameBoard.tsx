import { useEffect, useState } from "react";
import { GameStateProvider } from "../apiHelper/GameStateProvider.ts";
import MenuBar from "../components/gameboard/MenuBar.tsx";
import ContractResultScreen from "../components/gameboard/ContractResultScreen.tsx";
import GameOverScreen from "../components/gameboard/GameOverScreen.tsx";
import ActiveGameBoard from "../components/gameboard/ActiveGameBoard.tsx";
import PopupOverlay from "../components/menu-popups/PopupOverlay.tsx";

const RESULT_SHOW_MS = 5000;

export default function GameBoard() {
  const [userDismissedResults, setUserDismissedResults] = useState(false);
  const gamePhase = GameStateProvider(
    (state) => state.publicState?.gameState.type,
  );
  const turncount = GameStateProvider(
    (state) => state.publicState?.history.turns.length ?? 0,
  );
  const pendingPlayAreaDisplay = GameStateProvider(
    (state) => state.pendingPlayAreaDisplay,
  );
  const pendingContractResult = GameStateProvider(
    (state) => state.pendingContractResult,
  );
  const activePopup = GameStateProvider((state) => state.activePopup);
  const setPopup = GameStateProvider((state) => state.setPopup);

  // resets the dismissal flag whenever the phase changes
  useEffect(() => {
    setUserDismissedResults(false);
  }, [gamePhase]);

  const flush = GameStateProvider((state) => state.flushPendingUpdates);

  // auto-flush result screen after 5 seconds
  useEffect(() => {
    if (!pendingContractResult) return;
    const timer = setTimeout(() => {
      flush();
    }, RESULT_SHOW_MS);
    return () => clearTimeout(timer);
  }, [pendingContractResult, flush]);

  // determines which screen is rendered
  const renderMainContent = () => {
    // keep showing ActiveGameBoard until the completed play-area display clears
    if (pendingPlayAreaDisplay) return <ActiveGameBoard />;

    if (gamePhase === "GAME_OVER") return <GameOverScreen />;

    if (pendingContractResult) {
      return (
        <ContractResultScreen
          // Allow user to dismiss earlier than 5 seconds
          onClose={() => flush()}
        />
      );
    }

    // show if we are selecting a contract and  user hasn't clicked "Continue" yet
    if (
      gamePhase === "WAITING_FOR_CONTRACT_SELECTION" &&
      !userDismissedResults &&
      turncount > 0
    ) {
      return (
        <ContractResultScreen onClose={() => setUserDismissedResults(true)} />
      );
    }

    //default: actual game board
    return <ActiveGameBoard />;
  };

  return (
    <div className="relative size-full">
      <div className="absolute right-0 left-0 px-1 lg:px-2">
        <MenuBar />
      </div>

      <div className="flex h-full w-full">{renderMainContent()}</div>

      {/* conditionally rendered popup */}
      {activePopup && (
        <PopupOverlay type={activePopup} onClose={() => setPopup(null)} />
      )}
    </div>
  );
}
