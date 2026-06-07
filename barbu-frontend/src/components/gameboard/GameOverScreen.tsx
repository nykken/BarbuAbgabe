import H3 from "../ui/H3.tsx";
import ExitGameButton from "../ui/ExitGameButton.tsx";
import ScoresContent from "../menu-popups/ScoresContent.tsx";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import { useMemo } from "react";

export default function GameOverScreen() {
  const players = GameStateProvider((state) => state.lobbyState?.players);
  const publicState = GameStateProvider((state) => state.publicState);
  const activeVariant = GameStateProvider((state) => state.activeVariant);

  const winnerName = useMemo(() => {
    if (!players || !publicState || !activeVariant) return "Unknown";

    if (publicState.gameState.type !== "GAME_OVER") return "Unknown";

    const scores = publicState.gameState.finalScores;

    const sorted = [...players].sort((a, b) => {
      const scoreA = scores[a.position] ?? 0;
      const scoreB = scores[b.position] ?? 0;

      return activeVariant.rankingOrder === "HIGHEST_SCORE_WINS"
        ? scoreB - scoreA
        : scoreA - scoreB;
    });

    const winner = sorted[0];

    return winner?.type === "HUMAN" ? winner.username : `${winner.botName} (Bot)`;
  }, [players, publicState, activeVariant]);

  return (
    <div className="m-5 flex flex-1 flex-col items-center justify-center gap-3 md:m-8 md:gap-5 lg:m-20 lg:gap-7">
      <H3 text={`Game ended! Congratulations ${winnerName}!`} />
      <div className="w-2/3">
        <ScoresContent />
      </div>
      <ExitGameButton skipConfirm />
    </div>
  );
}
