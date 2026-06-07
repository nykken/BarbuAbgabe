import P from "../ui/P.tsx";
import H3 from "../ui/H3.tsx";
import ScoresContent from "../menu-popups/ScoresContent.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import { getContractEndingText } from "../../apiHelper/ContractLogic.ts";
import type { Scoring } from "../../apiHelper/VariantTypes.ts";

export default function ContractResultScreen({
  onClose,
}: {
  onClose: () => void;
}) {
  const history = GameStateProvider((state) => state.publicState?.history);
  const activeVariant = GameStateProvider((state) => state.activeVariant);
  if (history === null || activeVariant === null) return;

  const endedContractId = history?.turns
    .at(-1)
    ?.playedContracts.at(-1)?.contractId;
  const endedContract = activeVariant.contracts.find(
    (contract) => contract.id === endedContractId,
  );

  const endedContractName = endedContract?.displayName;

  if (endedContract === undefined) return;
  let displayScoring: number | Scoring = 0;

  if (endedContract?.scoring && endedContract.scoring.length === 1)
    displayScoring = endedContract.scoring[0];

  if (endedContract.placementPoints)
    displayScoring = endedContract.placementPoints.filter(
      (p) => p !== 0,
    ).length;

  return (
    <div className="m-5 flex flex-1 flex-col items-center justify-center gap-3 md:m-8 md:gap-5 lg:m-20 lg:gap-7">
      <div className="flex flex-col items-center">
        <H3 text={`${endedContractName} contract ended!`} />
        <P>{getContractEndingText(displayScoring)}</P>
      </div>
      <div className="w-2/3">
        <ScoresContent showContractResults={true} />
      </div>
      <button
        onClick={onClose}
        className="group border-text hover:bg-text hover:text-text-hover m-0 block w-fit rounded-full border px-2 py-1 lg:border-2 lg:px-4 lg:py-2"
      >
        <P>Continue the Game</P>
      </button>
    </div>
  );
}
