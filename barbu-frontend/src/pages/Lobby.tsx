import SubmitButton from "../components/ui/SubmitButton.tsx";
import { GameStateProvider } from "../apiHelper/GameStateProvider.ts";
import { UserStateProvider } from "../apiHelper/UserStateProvider.ts";
import { stompService } from "../apiHelper/StompService.ts";
import { useState } from "react";
import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import ExitGameButton from "../components/ui/ExitGameButton.tsx";
import CopyButton from "../components/ui/CopyButton.tsx";
import P from "../components/ui/P.tsx";

export default function Lobby() {
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);
  const lobby = GameStateProvider((state) => state.lobbyState);
  const gameId = GameStateProvider((state) => state.gameId);
  const userInfo = UserStateProvider((state) => state.userInfo);

  const players = lobby?.players || [];
  const isLobbyFull = players.length === 4;
  const isLobbyLeader =
    players[0]?.type === "HUMAN" && players[0]?.username === userInfo?.username;

  const handleStartGame = () => {
    setErrorMessage(null);

    if (!gameId || !stompService.isConnected) {
      setErrorMessage("Cannot start game: Disconnected from server.");
      return;
    }
    try {
      stompService.send(`/app/games/${gameId.gameId}/lobby/start`);
      setStarting(true);
    } catch (error) {
      setErrorMessage("Error sending start game signal");
    }
  };

  return (
    <StartPagesLayout title="Game Lobby" home={true}>
      <div className="mb-26 flex flex-col items-center gap-3 md:gap-4">
        <div className="ml-8 flex items-center gap-2 text-gray-500">
          <span className="text-[14px] md:text-[16px]">{gameId?.gameId}</span>

          {gameId?.gameId && <CopyButton text={gameId.gameId} />}
        </div>

        <div className="w-90 rounded-lg border p-2 md:w-110 lg:border-2">
          <div className="mb-2 border-b pb-1 text-[14px] font-bold md:pb-2 md:text-[16px] lg:border-b-2">
            <P lobby={true} bold={true}>
              {" "}
              Players ({players.length}/4)
            </P>
          </div>
          <ul className="flex flex-col gap-2 md:gap-3">
            {players.map((player, idx) => {
              const isHuman = player.type === "HUMAN";
              const name = isHuman
                ? player.username
                : `${player.botName} (Bot)`;
              return (
                <li
                  key={isHuman ? player.username : `bot-${idx}`}
                  className="flex items-center justify-between rounded"
                >
                  <P bold={isHuman && player.username === userInfo?.username}>
                    {name}
                  </P>
                  <P grey={true}>
                    (
                    {{
                      NORTH: "Player 1",
                      EAST: "Player 2",
                      SOUTH: "Player 3",
                      WEST: "Player 4",
                    }[player.position] ?? player.position}
                    )
                  </P>
                </li>
              );
            })}
          </ul>
        </div>
        {errorMessage && (
          <div className="mb-4 text-center text-sm font-semibold text-red-500">
            {errorMessage}
          </div>
        )}
        <div className="absolute bottom-5 left-1/2 flex w-95 -translate-x-1/2 items-center gap-4 px-2 md:bottom-8 md:w-118 md:px-4">
          {isLobbyLeader && (
            <SubmitButton
              text={isLobbyFull ? "Start Game" : "Start Game with Bots"}
              onClick={handleStartGame}
              disabled={starting}
              rectangle={true}
              fullWidth={true}
            />
          )}

          <ExitGameButton rectangle={true} fullWidth={true} />
        </div>
      </div>
    </StartPagesLayout>
  );
}
