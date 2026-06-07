import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import RoutingButton from "../components/ui/RoutingButton.tsx";
import H1 from "../components/ui/H1.tsx";
import TextInputWithLabel from "../components/ui/TextInputWithLabel.tsx";
import SubmitButton from "../components/ui/SubmitButton.tsx";
import { UserStateProvider } from "../apiHelper/UserStateProvider.ts";
import type { LoginData } from "../apiHelper/UserTypes.ts";
import P from "../components/ui/P.tsx";
import {useState} from "react";

export default function Login(){
  const [hasTriedLogin, setHasTriedLogin] = useState(false);
  const error = UserStateProvider((state) => state.error);
  async function submitLogin(formData: FormData) {
    setHasTriedLogin(true);
    const data: LoginData = {
      username: formData.get("username") as string,
      password: formData.get("password") as string,
    };
    await UserStateProvider.getState().setLogin(data);
  }

  return (
    <StartPagesLayout title="Login" login={true}>
      <form className="flex flex-col" action={submitLogin}>
        <div className="flex flex-col items-center gap-3 md:gap-5 lg:gap-7">
          <H1 text="Barbu" />
          <div className="flex w-full flex-col gap-2 md:gap-4 md:px-3 lg:px-8">
            <TextInputWithLabel
              labelText="Enter your Username:"
              id="username"
            />
            <TextInputWithLabel
              labelText="Enter your password:"
              id="password"
              password={true}
            />
            {hasTriedLogin && error && <P error={true}>{error}</P>}
            <div className="mt-2 flex w-full justify-between">
              <SubmitButton text="Log in" rectangle={true} />
              <RoutingButton
                text="Create an Account"
                url="/registration"
                rectangle={true}
              />
            </div>
          </div>
        </div>
      </form>
    </StartPagesLayout>
  );
}
