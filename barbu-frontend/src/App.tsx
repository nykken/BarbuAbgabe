import { Routes, Route, Navigate } from "react-router-dom";

// import pages
import Home from "./pages/Home.tsx";
import Ruleset from "./pages/Ruleset.tsx";
import JoinGame from "./pages/JoinGame.tsx";
import CreateGame from "./pages/CreateGame.tsx";
import Registration from "./pages/Registration.tsx";
import { UserStateProvider } from "./apiHelper/UserStateProvider.ts";

interface PrivateRouteProps {
  children: React.ReactNode;
}

function PrivateRoute({ children }: PrivateRouteProps) {
  const userInfo = UserStateProvider((state) => state.userInfo);

  // If not logged in, login redirect to login page
  if (!userInfo) {
    return <Navigate to="/" replace />; // "replace" prevents  hitting the back button to return to the protected route
  }

  return <>{children}</>;
}

/**
 * App component sets up all routes using React Router.
 * @constructor
 */

export default function App() {
  return (
    <Routes>
      {/*public*/}
      <Route path="/" element={<Home />} />
      <Route path="/registration" element={<Registration />} />
      {/*private*/}
      <Route
        path="/createGame"
        element={
          <PrivateRoute>
            <CreateGame />
          </PrivateRoute>
        }
      />
      <Route
        path="/ruleset"
        element={
          <PrivateRoute>
            <Ruleset />
          </PrivateRoute>
        }
      />
      <Route
        path="/joinGame"
        element={
          <PrivateRoute>
            <JoinGame />
          </PrivateRoute>
        }
      />
    </Routes>
  );
}
