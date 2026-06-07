import P from "../components/ui/P.tsx";
import H3 from "../components/ui/H3.tsx";
import HomeButton from "../components/ui/HomeButton.tsx";

interface JoinErrorProps {
  notExist?: boolean;
  maxPlayers?: boolean;
  inSession?: boolean;
}

export default function JoinError({
  notExist = true,
  maxPlayers = false,
  inSession = false,
}: JoinErrorProps) {
  return (
    <div className="flex size-full items-center justify-center">
      <div className="flex w-1/2 flex-col items-center gap-3 text-center sm:gap-5 lg:gap-7">
        <H3 text="Unable to connect to Lobby" />
        {notExist && (
          <P>
            The Lobby you are trying to reach does not exist or is no longer in
            Session.
          </P>
        )}
        {maxPlayers && (
          <P>
            The Lobby you are trying to reach is already at the maximum
            Playercount.
          </P>
        )}
        {inSession && (
          <P>The Lobby you are trying to reach is already in Session.</P>
        )}
        <P>
          Please press the Home-Button to return to the Titlescreen and then try
          joining a Lobby again.
        </P>
        <HomeButton />
      </div>
    </div>
  );
}
