import P from "./P.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import type { PlayerInfoDTO } from "../../apiHelper/GameTypes.ts";
import { playerColor } from "../../apiHelper/playerColorRecord.tsx";

export default function ScoresField({
  player,
  img,
  showContractResults = false,
}: {
  player: PlayerInfoDTO;
  img: string;
  showContractResults?: boolean;
}) {
  const gameState = GameStateProvider((state) => state.publicState?.gameState);
  const history = GameStateProvider((state) => state.publicState?.history);

  //determines current "total" points
  let points = 0;
  if (gameState?.type === "GAME_OVER") {
    points = gameState.finalScores[player.position] ?? 0;
  } else if (
    gameState?.type === "WAITING_FOR_CONTRACT_SELECTION" ||
    gameState?.type === "CONTRACT_IN_PROGRESS"
  ) {
    points = gameState.cumulativeScores[player.position] ?? 0;
  }

  //determines last contract points if needed
  let lastContractPoints = 0;
  if (showContractResults && history?.turns) {
    //find the last played contract in the most recent turn
    const currentTurn = history.turns.at(-1);
    const lastContract = currentTurn?.playedContracts.at(-1);
    lastContractPoints = lastContract?.scores[player.position] ?? 0;
  }

  const name = player.type === "HUMAN" ? player.username : `${player.botName} (Bot)`;

  const colorClass = playerColor[player.position];

  let sign = "+";
  let absContractPoints = 0;
  if (showContractResults) {
    sign = lastContractPoints < 0 ? "-" : "+";
    absContractPoints = Math.abs(lastContractPoints);
  }

  return (
    <div className="flex flex-row items-center">
      <div className="bg-text z-10 flex h-10 w-10 items-center justify-center rounded-full">
        <img
          src={img}
          alt="Ranking"
          className="bg-text w-7 rounded-full border-2"
        />
      </div>
      <div
        className={`border-text -ml-10 flex w-full flex-row items-center justify-between rounded-full border ${colorClass}`}
      >
        <div
          className={`flex w-full flex-row justify-between rounded-full py-1 pr-2 pl-12`}
        >
          <P>{name}</P>

          {showContractResults ? (
            <div className="flex w-26 gap-2 sm:w-32 lg:w-40">
              <div className="flex-2">
                <P>{points - lastContractPoints} Points</P>
              </div>
              <div className="flex-1">
                <P>
                  {sign} {absContractPoints}
                </P>
              </div>
            </div>
          ) : (
            <div className="w-18 md:w-24 lg:w-28">
              <P>{points} Points</P>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
