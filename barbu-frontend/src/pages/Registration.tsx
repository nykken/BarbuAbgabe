import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import TextInputWithLabel from "../components/ui/TextInputWithLabel.tsx";
import SubmitButton from "../components/ui/SubmitButton.tsx";
import { useNavigate } from "react-router";
import { useState } from "react";
import P from "../components/ui/P.tsx";
import type { LoginData } from "../apiHelper/UserTypes.ts";
import { UserStateProvider } from "../apiHelper/UserStateProvider.ts";

export default function Registration() {
  const [error, setError] = useState("");
  const navigate = useNavigate();

  async function submitRegistration(formData: FormData) {
    setError(""); //reset error message
    const password = formData.get("password");
    const confirm = formData.get("password-confirm");

    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    const data: LoginData = {
      username: formData.get("username") as string,
      password: formData.get("password") as string,
    };

    const success = await UserStateProvider.getState().setRegister(data);
    if (success) {
      navigate("/", { replace: true });
    }
  }

  return (
    <StartPagesLayout title="Registration">
      <form
        className="flex w-1/2 flex-col items-center justify-center gap-2 md:gap-3 lg:gap-5 xl:w-1/3"
        action={submitRegistration}
      >
        <TextInputWithLabel
          labelText="Enter your Username:"
          id="username"
          minLength={3}
          required={true}
        />
        <TextInputWithLabel
          labelText="Enter your password:"
          id="password"
          password={true}
          minLength={8}
          required={true}
        />
        <TextInputWithLabel
          labelText="Confirm your password:"
          id="password-confirm"
          password={true}
          minLength={8}
          required={true}
        />
        {error && <P error={true}>{error}</P>}
        <div className="mt-1 lg:mt-2 xl:mt-4">
          <SubmitButton text="Register" rectangle={true} />
        </div>
      </form>
    </StartPagesLayout>
  );
}
