import { UserStateProvider } from "../apiHelper/UserStateProvider.ts";
import StartPage from "./StartPage.tsx";
import Login from "./Login.tsx";
import { GameStateProvider } from "../apiHelper/GameStateProvider.ts";
import GameBoard from "./GameBoard.tsx";
import Lobby from "./Lobby.tsx";
import { useEffect } from "react";

export default function Home() {
  const userInfo = UserStateProvider((state) => state.userInfo);
  const activeGame = GameStateProvider((state) => state.gameId);
  const publicState = GameStateProvider((state) => state.publicState);
  const privateState = GameStateProvider((state) => state.privateState);
  const lobbyState = GameStateProvider((state) => state.lobbyState);
  const isQuickStart = GameStateProvider((state) => state.isQuickStart);

  const getUserInfo = UserStateProvider((state) => state.getUserInfo);
  const getActiveGame = GameStateProvider((state) => state.getActiveGame);

  useEffect(() => {
    if (userInfo == null) {
      getUserInfo();
    }
  }, [userInfo, getUserInfo]);

  useEffect(() => {
    if (userInfo != null && activeGame == null) {
      getActiveGame();
    }
  }, [userInfo, activeGame, getActiveGame]);

  if (!userInfo) {
    return <Login />;
  }

  if (!activeGame) {
    return <StartPage />;
  }

  if (publicState && privateState && lobbyState) {
    return <GameBoard />;
  }

  if (!publicState && !isQuickStart) {
    return <Lobby />;
  }

  return (
    <div className="flex h-full w-full items-center justify-center">
      <p className="text-gray-400">Starting game…</p>
    </div>
  );
}
