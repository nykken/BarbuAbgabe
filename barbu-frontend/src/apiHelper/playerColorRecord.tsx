import type { Player } from "./GameTypes";

export const playerColor: Record<Player, string> = {
  NORTH: "bg-playerN",
  WEST: "bg-playerW",
  EAST: "bg-playerE",
  SOUTH: "bg-playerS",
};
