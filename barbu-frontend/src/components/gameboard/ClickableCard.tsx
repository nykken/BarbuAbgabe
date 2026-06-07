import cardRecord from "../../apiHelper/CardRecord.tsx";

export default function ClickableCard({
  suit,
  rank,
  isSelected,
  onClick,
  isPlayable,
}: {
  suit: string;
  rank: string;
  isSelected: boolean;
  onClick?: () => void;
  isPlayable: boolean;
}) {
  const imageKey = isPlayable ? `${suit}${rank}` : `${suit}${rank}Grey`;
  const src = cardRecord[imageKey];

  return (
    <img
      src={src}
      alt={suit + rank + " card"}
      onClick={onClick}
      className={`-mr-5.5 h-[120%] last:mr-0 sm:-mr-5 md:-mr-7 lg:h-[130%] xl:-mr-4 xl:h-[150%] ${isSelected && "-mt-3 md:-mt-4 lg:-mt-8"} ${isPlayable && "cursor-pointer"} `}
    />
  );
}
