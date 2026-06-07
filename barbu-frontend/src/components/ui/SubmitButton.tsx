import P from "./P.tsx";
import { UserStateProvider } from "../../apiHelper/UserStateProvider.ts";

export default function SubmitButton({
  text,
  rectangle = false,
  fullWidth = false,
  logout = false,
  onClick,
  disabled = false,
  small = false,
}: {
  text: string;
  rectangle?: boolean;
  fullWidth?: boolean;
  logout?: boolean;
  onClick?: () => void;
  disabled?: boolean;
  small?: boolean;
}) {
  const handleClick = () => {
    if (disabled) {
      return;
    }
    if (logout) {
      UserStateProvider.getState().setLogout();
    } else if (onClick) {
      onClick();
    }
  };

  return (
    <button
      className={`group border-text hover:bg-text m-0 block h-fit border text-center lg:border-2 lg:px-3 lg:py-2 ${small ? "px-1 py-0" : "px-2 py-1"} ${
        fullWidth ? "w-full" : "w-fit"
      } ${
        rectangle ? "rounded-xs sm:rounded-sm lg:rounded-lg" : "rounded-full"
      } ${disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer"}`}
      type={logout || onClick ? "button" : "submit"}
      onClick={handleClick}
      disabled={disabled}
    >
      <P>{text}</P>
    </button>
  );
}
