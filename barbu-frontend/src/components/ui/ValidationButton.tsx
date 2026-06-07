import { NavLink } from "react-router";
import P from "./P.tsx";

interface ValidationButtonProps {
  text: string;
  url: string;
  disabled?: boolean;
  className?: string;
  onClick?: () => void;
}

export default function ValidationButton({
  text,
  url,
  disabled = false,
  className = "",
  onClick,
}: ValidationButtonProps) {
  return (
    <NavLink
      to={disabled ? "#" : url}
      className="text-xl"
      onClick={!disabled ? onClick : undefined}
    >
      <button
        disabled={disabled}
        className={`group border-text rounded-full border-2 px-4 py-2 ${disabled ? "cursor-not-allowed bg-black/8" : "hover:bg-text hover:text-text-hover"} ${className}`}
      >
        <P>{text}</P>
      </button>
    </NavLink>
  );
}
