import { useNavigate } from "react-router";
import homeIcon from "../../assets/home-icon.svg";
import homeIconWhite from "../../assets/home-icon-white.svg";

export default function HomeButton() {
  const navigate = useNavigate();

  function navigateHome() {
    navigate("/home");
  }

  return (
    <button
      onClick={navigateHome}
      className="group border-text hover:bg-text h-7 w-7 rounded-full border p-0.5 sm:h-8 sm:w-8 md:h-10 md:w-10 lg:h-11 lg:w-11 lg:border-2"
    >
      <img
        src={homeIcon}
        alt="home-button"
        className="block group-hover:hidden"
      />
      <img
        src={homeIconWhite}
        alt="home-button"
        className="hidden group-hover:block"
      />
    </button>
  );
}
