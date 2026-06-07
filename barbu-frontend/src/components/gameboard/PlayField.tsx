import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import ContractSelectionForm from "./ContractSelectionForm.tsx";
import TrickContractPlayfield from "./TrickContractPlayfield.tsx";
import ReussiteContractPlayfield from "./ReussiteContractPlayfield.tsx";

export default function PlayField() {
  const gamePhase = GameStateProvider(
    (state) => state.publicState?.gameState?.type,
  );

  const pendingPlayAreaDisplay = GameStateProvider(
    (state) => state.pendingPlayAreaDisplay,
  );

  const tableType = GameStateProvider((state) => {
    const gameState = state.publicState?.gameState;
    if (gameState && "tableState" in gameState) {
      return gameState.tableState.type;
    }
    return null;
  });

  return (
    <div className="size-full p-5">
      {gamePhase === "WAITING_FOR_CONTRACT_SELECTION" &&
        !pendingPlayAreaDisplay && <ContractSelectionForm />}
      {gamePhase === "CONTRACT_IN_PROGRESS" && tableType === "TRICK" && (
        <TrickContractPlayfield />
      )}

      {gamePhase === "CONTRACT_IN_PROGRESS" && tableType === "TABLEAU" && (
        <ReussiteContractPlayfield />
      )}
    </div>
  );
}
