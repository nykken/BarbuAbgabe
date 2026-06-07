type ScoringType = "SUIT" | "TRICKS" | "RANK" | "CARD" | "LAST_TWO";

export interface Scoring {
  type: ScoringType;
  suit?: string;
  rank?: string;
  pointsPerCard?: number;
  pointsPerTrick?: number;
  pointsPerRank?: number;
  points?: number;
  pointsSecondLast?: number;
  pointsLast?: number;
}

export interface LeadRestriction {
  type: "OPENING_PHASE" | "BROKEN_SUIT";
  suit?: string;
  length?: number;
}

export interface Contract {
  type: "TRICK_TAKING" | "REUSSITE";
  id: string;
  displayName: string;
  scoring?: Scoring[];
  leadRestriction?: LeadRestriction[];
  startingRank?: string;
  replayAfterRank?: string;
  placementPoints?: number[];
}

export interface Variant {
  id: string;
  displayName: string;
  deckVariant: string;
  rankingOrder: string;
  contracts: Contract[];
}

export const DECK_NAMES: Record<string, string> = {
  FROM_SEVEN: "32 Cards - From 7 to ace",
  STANDARD: "52 Cards - From 2 to ace",
};

export const RANKING_ORDERS: Record<string, string> = {
  LOWEST_SCORE_WINS: "The Player with the lowest score wins.",
  HIGHEST_SCORE_WINS: "The Player with the highest score wins.",
};

export const SUITS: Record<string, string> = {
  H: "hearts",
  C: "clubs",
  D: "diamonds",
  S: "spades",
};

export const RANKS: Record<string, string> = {
  J: "jack",
  Q: "queen",
  K: "king",
  A: "ace",
};
