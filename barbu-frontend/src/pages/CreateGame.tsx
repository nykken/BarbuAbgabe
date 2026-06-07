import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import RadioButtonWithButtonLabel from "../components/ui/RadioButtonWithButtonLabel.tsx";
import SubmitButton from "../components/ui/SubmitButton.tsx";
import P from "../components/ui/P.tsx";
import { VariantsProvider } from "../apiHelper/VariantsProvider.ts";
import type { GameCreationData } from "../apiHelper/GameTypes.ts";
import { GameStateProvider } from "../apiHelper/GameStateProvider.ts";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { PopupType } from "../apiHelper/Types.ts";
import PopupOverlay from "../components/menu-popups/PopupOverlay.tsx";

const difficulties: { label: string; value: "EASY" | "MEDIUM" | "HARD" }[] = [
  { label: "easy", value: "EASY" },
  { label: "medium", value: "MEDIUM" },
  { label: "hard", value: "HARD" },
];

export default function CreateGame() {
  const gamemodes: string[] = ["singleplayer", "multiplayer"];
  const variants = VariantsProvider((state) => state.variants);
  const fetchVariants = VariantsProvider((state) => state.getVariants);
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [selectedMode, setSelectedMode] = useState<string | null>(null);
  const activePopup = GameStateProvider((state) => state.activePopup);
  const setPopup = GameStateProvider((state) => state.setPopup);

  useEffect(() => {
    //initializes the variants once
    if (variants === null) {
      fetchVariants();
    }
  }, [variants, fetchVariants]);

  async function submitCreateGame(formData: FormData) {
    const mode = formData.get("gamemode");
    setErrorMessage(null);
    setSubmitting(true);

    const data: GameCreationData = {
      variantId: formData.get("variant") as string,
    };

    let success = null;
    if (mode === "singleplayer") {
      const difficulty = formData.get("botDifficulty") as GameCreationData["botDifficulty"];
      data.botDifficulty = difficulty ?? "MEDIUM";
      success = await GameStateProvider.getState().quickStart(data);
    } else if (mode === "multiplayer") {
      success = await GameStateProvider.getState().gameStart(data);
    }

    if (success && success.gameId) {
      navigate("/", { replace: true });
    } else {
      setErrorMessage("Game creation failed.");
      setSubmitting(false);
    }
  }

  return (
    <StartPagesLayout title="Create a new Game!">
      {variants === null ? (
        <P error={true}>Loading Data...</P>
      ) : (
        <form
          className="flex w-2/3 flex-col gap-3 sm:w-1/2 sm:gap-5 md:gap-6"
          action={submitCreateGame}
        >
          <div className="flex flex-col md:gap-1">
            <P>Select Mode:</P>
            <div className="flex flex-wrap gap-1 md:gap-2 lg:mt-1 xl:mt-2">
              {gamemodes.map((gamemode) => (
                <RadioButtonWithButtonLabel
                  key={gamemode}
                  id={gamemode}
                  text={gamemode}
                  groupName="gamemode"
                  onChange={() => setSelectedMode(gamemode)}
                />
              ))}
            </div>
          </div>

          {selectedMode === "singleplayer" && (
            <div className="flex flex-col md:gap-1">
              <P>Select Difficulty:</P>
              <div className="flex flex-wrap gap-1 md:gap-2 lg:mt-1 xl:mt-2">
                {difficulties.map(({ label, value }) => (
                  <RadioButtonWithButtonLabel
                    key={value}
                    id={value}
                    text={label}
                    groupName="botDifficulty"
                    defaultChecked={value === "MEDIUM"}
                  />
                ))}
              </div>
            </div>
          )}

          <div className="flex flex-col md:gap-1">
            <P>Select Variant:</P>
            <div className="flex flex-wrap items-center gap-1 md:gap-2 lg:mt-1 xl:mt-2">
              {variants.map((variant) => (
                <RadioButtonWithButtonLabel
                  key={variant.id}
                  id={variant.id}
                  text={variant.displayName}
                  groupName="variant"
                />
              ))}
              <div className="ml-1 md:ml-2 lg:ml-3">
                <SubmitButton
                  text="rules"
                  onClick={() => setPopup(PopupType.GameRules)}
                  rectangle={true}
                  small={true}
                />
              </div>
            </div>
          </div>
          {errorMessage && (
            <div className="mb-4 text-center text-sm font-semibold text-red-500">
              {errorMessage}
            </div>
          )}
          <div className="lg:mt-2 xl:mt-4">
            <SubmitButton
              text="Create Game"
              disabled={submitting}
              rectangle={true}
              fullWidth={true}
            />
          </div>
        </form>
      )}
      {activePopup && (
        <PopupOverlay
          type={activePopup}
          onClose={() => setPopup(null)}
          fullSize={true}
        />
      )}
    </StartPagesLayout>
  );
}