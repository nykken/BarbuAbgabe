import type { Position } from "../../apiHelper/Types.ts";
import { motion } from "motion/react";

export default function TrickCardImages({
  position,
  src,
  zIndex,
  winnerPosition,
}: {
  position: Position;
  src: string;
  zIndex: number;
  winnerPosition?: Position | null;
}) {
  const isMe = position === "bottom";

  const startPos = getPositionOffsets(position);
  const exitPos = winnerPosition ? getPositionOffsets(winnerPosition) : null;

  return (
    <div
      style={{ zIndex }}
      className={`absolute ${
        position === "left" &&
        "-translate-x-[1.875rem] sm:-translate-x-[2.125rem] md:-translate-x-[2.5rem] lg:-translate-x-[3.2rem] xl:-translate-x-[4rem]"
      } ${
        position === "right" &&
        "translate-x-[1.875rem] sm:translate-x-[2.125rem] md:translate-x-[2.5rem] lg:translate-x-[3.2rem] xl:translate-x-[4rem]"
      } ${
        position === "top" &&
        "-translate-y-[2.475rem] sm:-translate-y-[2.805rem] md:-translate-y-[3.3rem] lg:-translate-y-[4.235rem] xl:-translate-y-[5.28rem]"
      } ${
        position === "bottom" &&
        "translate-y-[2.475rem] sm:translate-y-[2.805rem] md:translate-y-[3.3rem] lg:translate-y-[4.235rem] xl:translate-y-[5.28rem]"
      }`}
    >
      <motion.img
        src={src}
        alt={`${position} player card`}
        initial={{
          x: startPos.x,
          y: startPos.y,
          rotateY: isMe ? 0 : 180,
          opacity: 0,
          scale: 0.5,
        }}
        animate={
          exitPos
            ? { x: exitPos.x, y: exitPos.y, opacity: 0, scale: 0.5 }
            : { x: 0, y: 0, rotateY: 0, opacity: 1, scale: 1 }
        }
        transition={
          exitPos
            ? { duration: 0.5, ease: "easeIn" }
            : { duration: 0.4, ease: "easeOut" }
        }
        className="h-[4.5rem] transform sm:h-[5.1rem] md:-mt-4 md:h-[6rem] lg:h-[7.7rem] xl:h-[9.6rem]"
      />
    </div>
  );
}

function getPositionOffsets(position: Position) {
  const innerWidth = typeof window !== "undefined" ? window.innerWidth : 1000;
  const innerHeight = typeof window !== "undefined" ? window.innerHeight : 1000;

  const xDistance = innerWidth / 2 + 100;
  const yDistance = innerHeight / 2 + 100;

  switch (position) {
    case "bottom":
      return { y: yDistance, x: 0 };
    case "top":
      return { y: -yDistance, x: 0 };
    case "left":
      return { x: -xDistance, y: 0 };
    case "right":
      return { x: xDistance, y: 0 };
    default:
      return { x: 0, y: 0 };
  }
}
