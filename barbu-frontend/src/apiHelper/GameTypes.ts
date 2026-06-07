/**
 * form data
 */
export interface GameCreationData {
  variantId: string;
  botDifficulty?: "EASY" | "MEDIUM" | "HARD";
}

export interface GameId {
  gameId: string;
}

/**
 * player info
 */
export type PlayerInfoDTO = HumanPlayer | BotPlayer;

export interface HumanPlayer {
  type: "HUMAN";
  position: Player;
  username: string;
}

export interface BotPlayer {
  type: "BOT";
  position: Player;
  botType: BotType;
  botName: string;
}

export type BotType = "EASY" | "MEDIUM" | "HARD";
export type Player = "NORTH" | "EAST" | "SOUTH" | "WEST";

/**
 * card info
 */
export interface Card {
  suit: Suit;
  rank: Rank;
}
export type Suit = "C" | "D" | "H" | "S";
export type Rank =
  | "2"
  | "3"
  | "4"
  | "5"
  | "6"
  | "7"
  | "8"
  | "9"
  | "10"
  | "J"
  | "Q"
  | "K"
  | "A";

export interface SuitPile {
  low: Rank;
  high: Rank;
}

/**
 * lobby state interface
 */
export interface Lobby {
  players: PlayerInfoDTO[];
}

export interface CompletedTrickDTO {
  type: "TRICK";
  cards: Record<Player, Card>;
  winner: Player;
}

export interface CompletedTableauDTO {
  type: "TABLEAU";
  piles: Partial<Record<Suit, SuitPile>>;
  lastPlayer: Player;
}

export type CompletedPlayAreaDTO = CompletedTrickDTO | CompletedTableauDTO;

/**
 * public state interface
 */
export interface PublicState {
  move: OutgoingMoveDTO;
  gameState: GameStateDTO;
  history: GameHistoryDTO;
  completedPlayArea: CompletedPlayAreaDTO | null;
}

export type OutgoingMoveDTO =
  | { type: "SELECT_CONTRACT"; declarer: Player }
  | { type: "PLAY_CARD"; player: Player; card: Card }
  | { type: "PASS"; player: Player };

export type GameStateDTO =
  | WaitingForContractSelection
  | ContractInProgress
  | GameOver;

export interface WaitingForContractSelection {
  type: "WAITING_FOR_CONTRACT_SELECTION";
  currentDeclarer: Player;
  cumulativeScores: Record<Player, number>;
  cardCounts: Record<Player, number>;
  availableContractIds: string[];
}

export interface ContractInProgress {
  type: "CONTRACT_IN_PROGRESS";
  currentDeclarer: Player;
  currentPlayer: Player;
  contractId: string;
  tableState: PlayAreaDTO;
  cumulativeScores: Record<Player, number>;
  cardCounts: Record<Player, number>;
}

export interface GameOver {
  type: "GAME_OVER";
  finalScores: Record<Player, number>;
}

export type PlayAreaDTO = TrickDTO | TableauDTO;

export interface TrickDTO {
  type: "TRICK";
  cardsPlayed: Record<Player, Card>;
  tricksTaken: Record<Player, number>;
}

export interface TableauDTO {
  type: "TABLEAU";
  piles: Record<Suit, SuitPile>;
}

export interface GameHistoryDTO {
  turns: DeclarerTurnResultDTO[];
}

export interface DeclarerTurnResultDTO {
  declarer: Player;
  playedContracts: ContractResultDTO[];
}

export interface ContractResultDTO {
  contractId: string;
  scores: Record<Player, number>;
}

/**
 * private state interface
 */
export interface PrivateState {
  hand?: Card[];
  legalMoves?: Card[];
}
