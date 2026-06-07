import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import SubmitButton from "../components/ui/SubmitButton.tsx";
import TextInputWithLabel from "../components/ui/TextInputWithLabel.tsx";
import { useNavigate } from "react-router";
import { GameStateProvider } from "../apiHelper/GameStateProvider.ts";
import { useState } from "react";

export default function JoinGame() {
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function submitJoinGame(formData: FormData) {
    setErrorMessage(null);

    const sessionId = formData.get("sessionId") as string;

    if (!sessionId) {
      setErrorMessage("Please enter a Session ID.");
      return;
    }

    const success = await GameStateProvider.getState().joinGame(sessionId);
    if (success) {
      navigate(`/`, { replace: true });
    } else {
      const storeError = GameStateProvider.getState().error;
      setErrorMessage(
        storeError || "Could not join the game. Please check the Session ID.",
      );
    }
  }

  return (
    <StartPagesLayout title="Join a Game!">
      <form
        className="flex w-1/2 flex-col items-center gap-3 sm:gap-5 lg:gap-7"
        action={submitJoinGame}
      >
        <TextInputWithLabel labelText="Enter game session:" id="sessionId" />
        {errorMessage && (
          <div className="mb-4 text-center text-sm font-semibold text-red-500">
            {errorMessage}
          </div>
        )}
        <div className="mt-1 md:mt-2">
          <SubmitButton text="Join Game" rectangle={true} />
        </div>
      </form>
    </StartPagesLayout>
  );
}
