import RoutingButton from "../components/ui/RoutingButton.tsx";
import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import H1 from "../components/ui/H1.tsx";
import P from "../components/ui/P.tsx";
import { UserStateProvider } from "../apiHelper/UserStateProvider.ts";
import SubmitButton from "../components/ui/SubmitButton.tsx";

export default function StartPage() {
  const userInfo = UserStateProvider((state) => state.userInfo);

  return (
    <StartPagesLayout title="Barbu" home={true}>
      <div className="absolute top-0 right-0 flex w-fit flex-col gap-1">
        <RoutingButton
          text="Rules"
          url="/ruleset"
          rectangle={true}
        ></RoutingButton>
        <SubmitButton text="logout" logout={true} />
      </div>
      <div className="flex flex-col items-center justify-center gap-5 sm:gap-7 lg:gap-16">
        <div className="flex flex-col items-center justify-center gap-2 lg:gap-3">
          <H1 text="Barbu" />
          {userInfo && <P>Logged in as {userInfo.username}</P>}
        </div>
        <div className="m-0 flex flex-col gap-3 self-center p-0 sm:gap-4 lg:gap-6">
          <RoutingButton
            text="Create a Game"
            url="/creategame"
            fullWidth={true}
          />
          <RoutingButton
            text="Join an ongoing Session"
            url="/joingame"
            fullWidth={true}
          />
        </div>
      </div>
    </StartPagesLayout>
  );
}
