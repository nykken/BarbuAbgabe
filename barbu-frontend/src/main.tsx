import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./main.css";
import App from "./App.tsx";
import GameContainer from "./components/gameboard/GameContainer.tsx";

const root = document.getElementById("root");

if (root == null) {
  throw new Error("Root element #root not found");
} else {
  createRoot(root).render(
    <GameContainer>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </GameContainer>,
  );
}
