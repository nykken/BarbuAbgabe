import { useEffect } from "react";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import P from "../ui/P.tsx";
import VariantDetails from "../variants-and-contracts/VariantDetails.tsx";
import ContractDetails from "../variants-and-contracts/ContractDetails.tsx";

export default function RulesContent() {
  const gameId = GameStateProvider((state) => state.gameId);
  const activeVariant = GameStateProvider((state) => state.activeVariant);
  const fetchVariant = GameStateProvider(
    (state) => state.getVariantForActiveGame,
  );
  const gameState = GameStateProvider((state) => state.publicState?.gameState);
  const gamePhase = gameState?.type;

  useEffect(() => {
    //ensures the variant is loaded
    if (gameId && !activeVariant) {
      fetchVariant(gameId);
    }
  }, [gameId, activeVariant, fetchVariant]);

  if (activeVariant === null) return <P error={true}>Loading rules...</P>;

  if (gamePhase === "WAITING_FOR_CONTRACT_SELECTION") {
    return (
      <div className="custom-scrollbar min-h-0 flex-1 overflow-y-auto pr-2">
        <VariantDetails variant={activeVariant} />
      </div>
    );
  }

  if (gamePhase === "CONTRACT_IN_PROGRESS") {
    const allContracts = activeVariant?.contracts ?? [];
    const currentContractId = gameState?.contractId;
    const currentContract = allContracts.find(
      (contract) => contract.id === currentContractId,
    );
    if (currentContract) return <ContractDetails contract={currentContract} />;
  }
}
