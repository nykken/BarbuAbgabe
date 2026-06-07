import { GameStateProvider } from "../../apiHelper/GameStateProvider.ts";
import { useNavigate } from "react-router";
import P from "./P.tsx";

export default function ExitGameButton({
  rectangle = false,
  fullWidth = false,
  skipConfirm = false,
}: {
  rectangle?: boolean;
  fullWidth?: boolean;
  skipConfirm?: boolean;
}) {
  const navigate = useNavigate();
  const gameId = GameStateProvider((state) => state.gameId);
  const players = GameStateProvider((state) => state.lobbyState?.players);

  let amountOfBotPlayers = 0;
  let confirmText = "";
  players?.map((player) => {
    if (player.type === "BOT") amountOfBotPlayers++;
  });

  if (amountOfBotPlayers === 3) {
    confirmText =
      "The game ends and your current progress will be lost. Do you really want to exit the game and return to the home screen?";
  } else {
    confirmText =
      "You will be replaced by a Bot and cannot rejoin this session. The game will continue without you. Do you really want to exit the game and return to the home screen?";
  }

  const handleExit = () => {
    if (skipConfirm || window.confirm(confirmText)) {
      if (gameId) GameStateProvider.getState().exitGame(gameId);
      navigate("/");
    }
  };
  return (
    <button
      onClick={handleExit}
      className={`group border-text hover:bg-text hover:text-text-hover m-0 block cursor-pointer border px-2 py-1 text-center lg:border-2 lg:px-3 lg:py-2 ${
        rectangle ? "rounded-xs sm:rounded-sm lg:rounded-lg" : "rounded-full"
      } ${fullWidth ? "w-full" : "w-fit"}`}
    >
      <P>Exit Game</P>
    </button>
  );
}
