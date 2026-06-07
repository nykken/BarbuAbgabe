import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import { useEffect } from "react";
import RadioButtonWithButtonLabel from "../ui/RadioButtonWithButtonLabel.tsx";
import SubmitButton from "../ui/SubmitButton.tsx";
import P from "../ui/P.tsx";
import { PopupType } from "../../apiHelper/Types.ts";

export default function ContractSelectionForm() {
  const gameId = GameStateProvider((state) => state.gameId);
  const selectContract = GameStateProvider((state) => state.selectContract);
  const activeVariant = GameStateProvider((state) => state.activeVariant);
  const fetchVariant = GameStateProvider(
    (state) => state.getVariantForActiveGame,
  );
  const publicState = GameStateProvider((state) => state.publicState);
  const setPopup = GameStateProvider((state) => state.setPopup);
  const myBackendPosition = GameStateProvider(
    (state) => state.positionMap?.["bottom"],
  );

  useEffect(() => {
    // fetches variant once
    if (gameId && !activeVariant) {
      fetchVariant(gameId);
    }
  }, [gameId, activeVariant, fetchVariant]);

  const availableContractIds: string[] = GameStateProvider((state) =>
    state.publicState?.gameState.type === "WAITING_FOR_CONTRACT_SELECTION"
      ? state.publicState.gameState.availableContractIds
      : [],
  );

  // Return null instead of undefined to prevent React crashes
  if (!activeVariant || !gameId || !publicState) return null;

  function submitContract(formData: FormData) {
    const contractId = formData.get("contract") as string;
    selectContract(contractId);
  }

  const isDeclarer =
    publicState.gameState.type === "WAITING_FOR_CONTRACT_SELECTION" &&
    publicState.gameState.currentDeclarer === myBackendPosition;

  return (
    <div className="flex size-full items-center justify-center">
      <form
        className="flex flex-col items-center gap-3"
        action={submitContract}
      >
        <div className="flex w-full items-center justify-between gap-3 px-2">
          {isDeclarer ? (
            <P>You are the declarer. Choose a contract!</P>
          ) : (
            <P>Wait for the declarer to choose a contract...</P>
          )}
          <SubmitButton
            text="HELP"
            onClick={() => setPopup(PopupType.VariantRules)}
            rectangle={true}
            small={true}
          />
        </div>

        <div className="my-1 flex flex-wrap justify-center gap-y-1 md:my-2 md:gap-y-2">
          {activeVariant.contracts.map((contract) => (
            <div key={contract.id} className="w-1/3 px-0.5 md:px-1">
              <RadioButtonWithButtonLabel
                id={contract.id}
                text={contract.displayName}
                disabled={!availableContractIds.includes(contract.id)}
                noHover={!isDeclarer}
                groupName="contract"
                small={true}
              />
            </div>
          ))}
        </div>

        {isDeclarer && <SubmitButton text="select" />}
      </form>
    </div>
  );
}
