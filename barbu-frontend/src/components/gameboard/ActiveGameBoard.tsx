import CardDeckOpponent from "./CardDeckOpponent.tsx";
import PlayField from "./PlayField.tsx";
import CardDeckPlayer from "./CardDeckPlayer.tsx";

export default function ActiveGameBoard() {
  return (
    <div className="flex size-full flex-col">
      <div className="flex flex-1 flex-col">
        <div className="flex h-5 items-center justify-center sm:h-6 md:h-7 lg:h-10 xl:h-12">
          <CardDeckOpponent position="top" />
        </div>

        <div className="flex flex-1">
          <div className="flex w-10 items-center justify-center sm:w-12 md:w-14 lg:w-20 xl:w-24">
            <CardDeckOpponent position="left" />
          </div>
          <div className="flex-1">
            <PlayField />
          </div>
          <div className="flex w-10 items-center justify-center sm:w-12 md:w-14 lg:w-20 xl:w-24">
            <CardDeckOpponent position="right" />
          </div>
        </div>
      </div>

      <div className="h-15 md:h-20 lg:h-22 xl:h-24">
        <CardDeckPlayer />
      </div>
    </div>
  );
}
