import { useNavigate } from "react-router";
import returnIcon from "../../assets/return-icon.svg";
import returnIconWhite from "../../assets/return-icon-white.svg";

export default function ReturnButton() {
  const navigate = useNavigate();

  function navigateBack() {
    navigate(-1);
  }

  return (
    <button
      onClick={navigateBack}
      className="group border-text hover:bg-text h-7 w-7 cursor-pointer rounded-full border p-0.5 sm:h-8 sm:w-8 md:h-10 md:w-10 lg:h-11 lg:w-11 lg:border-2"
    >
      <img
        src={returnIcon}
        alt="return-button"
        className="block group-hover:hidden"
      />
      <img
        src={returnIconWhite}
        alt="return-button"
        className="hidden group-hover:block"
      />
    </button>
  );
}
