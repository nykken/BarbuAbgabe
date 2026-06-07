import { NavLink } from "react-router";
import P from "./P.tsx";

export default function RoutingButton({
  text,
  url,
  rectangle = false,
  fullWidth = false,
}: {
  text: string;
  url: string;
  rectangle?: boolean;
  fullWidth?: boolean;
}) {
  return (
    <NavLink
      to={url}
      className={`group border-text hover:bg-text m-0 block h-fit border px-2 py-1 text-center lg:border-2 lg:px-3 lg:py-2 ${fullWidth ? "w-full" : "w-fit"} ${rectangle ? "rounded-sm sm:rounded-md lg:rounded-lg" : "rounded-full"}`}
    >
      <P>{text}</P>
    </NavLink>
  );
}
