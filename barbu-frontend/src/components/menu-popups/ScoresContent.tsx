import { useMemo } from "react";
import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import ScoresField from "../ui/ScoresField.tsx";
import P from "../ui/P.tsx";
import crownWhite from "../../assets/crown-white.svg";
import twoWhite from "../../assets/two-white.svg";
import threeWhite from "../../assets/three-white.svg";
import fourWhite from "../../assets/four-white.svg";

export default function ScoresContent({
  showContractResults = false,
}: {
  showContractResults?: boolean;
}) {
  const players = GameStateProvider((state) => state.lobbyState?.players);
  const publicState = GameStateProvider((state) => state.publicState);
  const activeVariant = GameStateProvider((state) => state.activeVariant);
  const rankingIcons = [crownWhite, twoWhite, threeWhite, fourWhite];

  const sortedPlayers = useMemo(() => {
    if (!players || !publicState || !activeVariant) return null;

    // Use a Type Guard to safely get scores
    const scores =
      publicState.gameState.type !== "GAME_OVER"
        ? publicState.gameState.cumulativeScores
        : publicState.gameState.finalScores;

    return [...players].sort((a, b) => {
      const scoreA = scores?.[a.position] ?? 0;
      const scoreB = scores?.[b.position] ?? 0;

      return activeVariant.rankingOrder === "HIGHEST_SCORE_WINS"
        ? scoreB - scoreA
        : scoreA - scoreB;
    });
  }, [players, publicState, activeVariant]); // only re-runs if these change

  if (!sortedPlayers) return <P>Loading scores...</P>;

  return (
    <div className="flex h-full flex-col justify-around gap-1">
      {sortedPlayers.map((player, index) => (
        <ScoresField
          key={player.position}
          player={player}
          img={rankingIcons[index]}
          showContractResults={showContractResults}
        />
      ))}
    </div>
  );
}
