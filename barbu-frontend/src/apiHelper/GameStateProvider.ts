import { create } from "zustand";
import type {
  Card,
  CompletedPlayAreaDTO,
  GameCreationData,
  GameId,
  Lobby,
  PrivateState,
  PublicState,
} from "./GameTypes.ts";
import { stompService } from "./StompService.ts";
import type { Variant } from "./VariantTypes.ts";
import { UserStateProvider } from "./UserStateProvider.ts";
import type { PopupType } from "./Types.ts";

const host = import.meta.env.VITE_API_BASE_URL || "localhost:8080";

const ANIMATION_STEP_MS = 1000;

let _publicStateBuffer: PublicState | null = null;

type QueueFrame =
  | { kind: "pair"; publicState: PublicState; privateState: PrivateState } // to synchronize public and private state for consistent UI
  | { kind: "playArea"; playArea: CompletedPlayAreaDTO; lastPlayedCard?: Card } // to display finished trick/reussite
  | { kind: "contractResult"; publicState: PublicState; privateState: PrivateState }; // to display contract result screen although someone already started playing

let _queue: QueueFrame[] = [];
let _timer: ReturnType<typeof setTimeout> | null = null;
let _waitingForFlush = false; // state lock

function processNext() {
  _timer = null;
  const frame = _queue.shift();
  if (!frame) return;
  if (frame.kind === "pair") {
    GameStateProvider.setState({
      publicState: frame.publicState,
      privateState: frame.privateState,
    });
    _timer = setTimeout(processNext, ANIMATION_STEP_MS);
  } else if (frame.kind === "playArea") {
    GameStateProvider.setState({
      pendingPlayAreaDisplay: frame.playArea,
      lastPlayedCard: frame.lastPlayedCard ?? null,
    });
    _waitingForFlush = true; // lock the queue
  } else if (frame.kind === "contractResult") {
    GameStateProvider.setState({
      pendingContractResult: frame.publicState,
      publicState: frame.publicState,
      privateState: frame.privateState,
    });
    _waitingForFlush = true;
  }
}

function enqueue(frame: QueueFrame) {
  _queue.push(frame);
  if (_timer === null && !_waitingForFlush) {
    processNext();
  }
}

function getCsrfToken(): string {
  return (
    document.cookie
      .split("; ")
      .find((r) => r.startsWith("XSRF-TOKEN="))
      ?.split("=")[1] ?? ""
  );
}

interface GameState {
  // state attributes
  gameId: GameId | null;
  isLoading: boolean;
  error: string | null;
  lobbyState: Lobby | null;
  publicState: PublicState | null;
  privateState: PrivateState | null;
  activeVariant: Variant | null;
  positionMap: Record<string, "NORTH" | "EAST" | "SOUTH" | "WEST"> | null;
  activePopup: PopupType | null;
  pendingPlayAreaDisplay: CompletedPlayAreaDTO | null;
  lastPlayedCard: Card | null;
  pendingContractResult: PublicState | null;
  isQuickStart: boolean;

  // actions
  getActiveGame: () => Promise<GameId | null>;
  initActiveGame: (gameId: GameId) => Promise<void>;
  exitGame: (gameId: GameId) => void;
  openWebsocketConnectionAndSubscribe: () => void;
  quickStart: (data: GameCreationData) => Promise<GameId | null>;
  gameStart: (data: GameCreationData) => Promise<GameId | null>;
  getVariantForActiveGame: (gameId: GameId) => Promise<Variant | null>;
  selectContract: (contractId: string) => void;
  joinGame: (gameIdStr: string) => Promise<boolean>;
  pass: () => void;
  playCard: (card: Card) => void;
  getContractRound: () => number;
  getMaxContractRounds: () => number;
  getContractName: () => string;
  getDeclarerRound: () => number;
  updatePositionMap: () => void;
  getPosByUiPos: (
    uiPos: string,
  ) => "NORTH" | "EAST" | "SOUTH" | "WEST" | undefined;
  getCardCountByUiPos: (uiPos: string) => number;
  getTrickCountByUiPos: (uiPos: string) => number;
  getIsActiveByUiPos: (uiPos: string) => boolean;
  getPlayerTypeByUiPos: (uiPos: string) => "HUMAN" | "BOT" | undefined;
  getSortedHand: () => Card[];
  checkCanPass: () => boolean;
  setPopup: (type: PopupType | null) => void;
  flushPendingUpdates: () => void;
}

export const GameStateProvider = create<GameState>((set, get) => ({
  gameId: null,
  isLoading: false,
  error: null,
  lobbyState: null,
  publicState: null,
  privateState: null,
  activeVariant: null,
  positionMap: null,
  activePopup: null,
  pendingPlayAreaDisplay: null,
  lastPlayedCard: null,
  pendingContractResult: null,
  isQuickStart: false,

  // finds active game (if existing) for user
  getActiveGame: async () => {
    if (get().isLoading) return null;
    set({ isLoading: true, error: null });
    try {
      const response = await fetch(`http://${host}/api/games/current`, {
        method: "GET",
        headers: {
          "X-XSRF-TOKEN": getCsrfToken(),
        },
        credentials: "include",
      });

      if (response.ok) {
        const result: GameId = await response.json();
        await GameStateProvider.getState().initActiveGame(result);
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "getActiveGame: Could not fetch active game.",
          isLoading: false,
        });
      }
    } catch (error: any) {
      set({
        error:
          "getActiveGame: Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
    }
    return get().gameId;
  },

  // wires up an already-known game id: stores it, opens the websocket
  // (subscribing to all channels), and fetches the variant. Shared by
  // getActiveGame (id from GET /current) and the create flows (id from POST).
  initActiveGame: async (gameId: GameId) => {
    set({ gameId, isLoading: false });
    GameStateProvider.getState().openWebsocketConnectionAndSubscribe();
    await GameStateProvider.getState().getVariantForActiveGame(gameId);
  },

  openWebsocketConnectionAndSubscribe: () => {
    if (stompService.isConnected) return;
    stompService.activate();

    const unsubscribeStatus = stompService.onStatusChange((connected) => {
      if (!connected) return;

      // clear any stale animation state from before the disconnect
      if (_timer !== null) {
        clearTimeout(_timer);
        _timer = null;
      }
      _waitingForFlush = false;
      _publicStateBuffer = null;
      _queue = [];
      GameStateProvider.setState({
        pendingPlayAreaDisplay: null,
        lastPlayedCard: null,
      });

      const gameId = get().gameId?.gameId;

      // snapshot subscriptions (one-time response to get current state)
      stompService.subscribe(`/app/games/${gameId}/lobby`, (msg) => {
        console.log("LOBBY SNAPSHOT", msg);
        set({ lobbyState: msg });
        get().updatePositionMap();
      });

      stompService.subscribe(`/app/games/${gameId}/public-state`, (msg) => {
        set({ publicState: msg });
      });

      stompService.subscribe(`/app/games/${gameId}/private-state`, (msg) => {
        set({ privateState: msg });
      });

      // broadcast subscriptions
      stompService.subscribe(`/topic/games/${gameId}/lobby`, (msg) => {
        console.log("LOBBY BROADCAST", msg);
        set({ lobbyState: msg });
        get().updatePositionMap();
      });

      stompService.subscribe(`/topic/games/${gameId}`, (msg: PublicState) => {
        if (msg.gameState.type === "GAME_OVER") {
          // No private state is sent for GAME_OVER; process immediately.
          if (msg.completedPlayArea) {
            const lastPlayedCard =
              msg.move.type === "PLAY_CARD" ? msg.move.card : undefined;
            enqueue({
              kind: "playArea",
              playArea: msg.completedPlayArea,
              lastPlayedCard,
            });
          }
          enqueue({ kind: "pair", publicState: msg, privateState: {} });
        } else {
          _publicStateBuffer = msg;
        }
      });

      stompService.subscribe(
        `/user/queue/games/${gameId}/private-state`,
        (msg: PrivateState) => {
          if (_publicStateBuffer === null) return;
          const publicState = _publicStateBuffer;
          _publicStateBuffer = null;

          // detect completed trick/tableau
          if (publicState.completedPlayArea) {
            const lastPlayedCard =
              publicState.move.type === "PLAY_CARD"
                ? publicState.move.card
                : undefined;
            enqueue({
              kind: "playArea",
              playArea: publicState.completedPlayArea,
              lastPlayedCard,
            });
          }

            // detect contract transition
            // If the buffered public state says we are selecting a contract,
            // but the PREVIOUS public state was in progress, we enqueue a pause.
            const currentState = GameStateProvider.getState().publicState;
            if (
              currentState?.gameState.type === "CONTRACT_IN_PROGRESS" &&
              publicState.gameState.type === "WAITING_FOR_CONTRACT_SELECTION"
            ) {
              // enqueue the 'Contract Finished' view as a buffered frame
              enqueue({ kind: "contractResult", publicState: publicState, privateState: msg });
            }

          enqueue({
            kind: "pair",
            publicState: publicState,
            privateState: msg,
          });
        },
      );

      stompService.subscribe(`/user/queue/games/${gameId}/error`, (message) => {
        if (message.body) {
          const errorData = JSON.parse(message.body);
          console.error("Game Error Received:", errorData);
        }
      });

      // stop listening for status changes once we've subscribed
      unsubscribeStatus();
    });
  },

  // creates single player game: uses the same endpoint as multiplayer, then auto-starts via STOMP
  quickStart: async (data: GameCreationData) => {
    if (get().isLoading) return null;
    set({ isLoading: true, error: null, isQuickStart: true });
    try {
      const response = await fetch(`http://${host}/api/games`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-XSRF-TOKEN": getCsrfToken(),
        },
        credentials: "include",
        body: JSON.stringify(data),
      });

      if (response.ok) {
        const result: GameId = await response.json();
        await get().initActiveGame(result);
        const gameId = result.gameId;
        if (gameId) {
          let startSent = false;
          stompService.onStatusChange((connected) => {
            if (!connected || startSent) return;
            startSent = true;
            stompService.send(`/app/games/${gameId}/lobby/start`);
          });
        }
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "quickStart: Could not create a new game.",
          isLoading: false,
        });
      }
    } catch (error: any) {
      set({
        error:
          "quickStart: Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
    }
    return get().gameId;
  },

  // creates multiplayer game
  gameStart: async (data: GameCreationData) => {
    if (get().isLoading) return null;
    set({ isLoading: true, error: null, isQuickStart: false });
    try {
      const response = await fetch(`http://${host}/api/games`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-XSRF-TOKEN": getCsrfToken(),
        },
        credentials: "include",
        body: JSON.stringify(data),
      });

      if (response.ok) {
        const result: GameId = await response.json();
        await get().initActiveGame(result);
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "gameStart: Could not create a new game.",
          isLoading: false,
        });
      }
    } catch (error: any) {
      set({
        error:
          "gameStart: Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
    }
    return get().gameId;
  },

  // join multiplayer game
  joinGame: async (gameIdStr: string) => {
    if (get().isLoading) return false;
    set({ isLoading: true, error: null });

    try {
      const response = await fetch(
        `http://${host}/api/games/${gameIdStr}/join`,
        {
          method: "POST",
          headers: {
            "X-XSRF-TOKEN": getCsrfToken(),
          },
          credentials: "include",
        },
      );

      if (response.ok) {
        set({ isLoading: false });
        await get().getActiveGame();
        return true;
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "Could not join game.",
          isLoading: false,
        });
        return false;
      }
    } catch (error: any) {
      set({
        error: "Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
      return false;
    }
  },

  // exit active Game
  exitGame: async (gameId) => {
    if (get().isLoading) return null;
    set({ isLoading: true, error: null });
    try {
      const response = await fetch(
        `http://${host}/api/games/${gameId.gameId}/leave`,
        {
          method: "POST",
          headers: {
            "X-XSRF-TOKEN": getCsrfToken(),
          },
          credentials: "include",
        },
      );

      if (response.ok) {
        stompService.deactivate();
        if (_timer !== null) {
          clearTimeout(_timer);
          _timer = null;
        }
        _waitingForFlush = false;
        _publicStateBuffer = null;
        _queue = [];
        set({
          gameId: null,
          isLoading: false,
          lobbyState: null,
          publicState: null,
          privateState: null,
          activeVariant: null,
          positionMap: null,
          activePopup: null,
          pendingPlayAreaDisplay: null,
          isQuickStart: false,
        });
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "quickState: Could not create a new game.",
          isLoading: false,
        });
      }
    } catch (error: any) {
      set({
        error:
          "quickState: Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
    }
  },

  // fetches the variant for the active game
  getVariantForActiveGame: async (gameId) => {
    if (get().isLoading) return null;
    set({ isLoading: true, error: null });
    try {
      const response = await fetch(
        `http://${host}/api/games/${gameId.gameId}/variant`,
        {
          method: "GET",
        },
      );
      if (response.ok) {
        const data: Variant = await response.json();
        set({ activeVariant: data, isLoading: false });
        return data;
      } else {
        const errorText = await response.text();
        set({
          error: errorText || "Could not get active game variant.",
          isLoading: false,
        });
      }
    } catch (error: any) {
      set({
        error: "Cannot connect to server. Please check your connection.",
        isLoading: false,
      });
    }
    return null;
  },

  // stomp move
  selectContract: (contractId: string) => {
    const gameId = get().gameId?.gameId;
    if (!gameId) return;

    const selectContractMove = {
      type: "SELECT_CONTRACT",
      contractId: contractId,
    };

    stompService.send(`/app/games/${gameId}/move`, selectContractMove);
  },

  // stomp move
  pass: () => {
    const gameId = get().gameId?.gameId;
    if (!gameId) return;

    const passMove = {
      type: "PASS",
    };

    stompService.send(`/app/games/${gameId}/move`, passMove);
  },

  // stomp move
  playCard: (card: Card) => {
    const gameId = get().gameId?.gameId;
    if (!gameId) return;

    const playCardMove = {
      type: "PLAY_CARD",
      card: {
        suit: card.suit,
        rank: card.rank,
      },
    };

    stompService.send(`/app/games/${gameId}/move`, playCardMove);
  },

  // calculate current contract round
  getContractRound: () => {
    const gameState = get().publicState?.gameState;
    const history = get().publicState?.history;
    const turns = history?.turns ?? [];
    const totalContractsInVariant = get().activeVariant?.contracts.length ?? 0;

    if (!gameState) return 0;

    if (gameState.type === "WAITING_FOR_CONTRACT_SELECTION") {
      // round = total - how many choices are still available + 1
      return (
        totalContractsInVariant - gameState.availableContractIds.length + 1
      );
    }

    if (gameState.type === "CONTRACT_IN_PROGRESS") {
      let currentContractRound = 0;
      // first contract in the first round (turns undefined)
      if (turns.length === 0) return 1;
      if (turns.at(-1)?.playedContracts.length === totalContractsInVariant) {
        // first contract in a new round
        currentContractRound = 1;
      } else {
        // already one finished contract in this round
        currentContractRound = (turns.at(-1)?.playedContracts.length ?? 0) + 1;
      }
      return currentContractRound;
    }

    // GAME_OVER
    return totalContractsInVariant;
  },

  // calculate max contract rounds
  getMaxContractRounds: () => {
    return get().activeVariant?.contracts.length ?? 0;
  },

  // get displayName for current contract
  getContractName: () => {
    const { publicState, activeVariant } = get();
    const gameState = publicState?.gameState;

    if (gameState?.type === "WAITING_FOR_CONTRACT_SELECTION") {
      return "Selecting...";
    }

    if (gameState?.type === "CONTRACT_IN_PROGRESS") {
      const contractId = gameState.contractId;
      const currentContract = activeVariant?.contracts.find(
        (contract) => contract.id === contractId,
      );
      return currentContract?.displayName ?? "Loading...";
    }

    // GAME_OVER
    return "all Contracts played";
  },

  // calculate current declarer round
  getDeclarerRound: () => {
    const history = get().publicState?.history;
    const turns = history?.turns ?? [];
    const totalContractsInVariant = get().activeVariant?.contracts.length ?? 0;
    let currentDeclarerRound = 0;

    // if there are no turns at all, we are definitely in the first declarer round
    if (turns.length === 0) return 1;

    if (turns.at(-1)?.playedContracts.length === totalContractsInVariant) {
      // first contract in a new round
      currentDeclarerRound = turns.length + 1;
    } else {
      // already one finished contract in this round
      currentDeclarerRound = turns.length;
    }

    return Math.min(currentDeclarerRound, 4);
  },

  // stores the backend position (NORTH, ...) to the matching UI player position (top, ...)
  updatePositionMap: () => {
    const players = get().lobbyState?.players || [];
    const userInfo = UserStateProvider.getState().userInfo;
    const username = userInfo?.username;

    if (!username || players.length === 0) return;

    const COMPASS = ["NORTH", "EAST", "SOUTH", "WEST"];
    const sorted = [...players].sort(
      (a, b) => COMPASS.indexOf(a.position) - COMPASS.indexOf(b.position),
    );

    const myIndex = sorted.findIndex(
      (player) => player.type === "HUMAN" && player.username === username,
    );
    if (myIndex === -1) return;

    //rotates the array so "bottom" is always the local player
    const rotated = [...sorted.slice(myIndex), ...sorted.slice(0, myIndex)];

    set({
      positionMap: {
        bottom: rotated[0]?.position, //always me
        left: rotated[1]?.position, //player to my left
        top: rotated[2]?.position, //player opposite me
        right: rotated[3]?.position, //player to my right
      },
    });
  },

  // returns backend position matching the given UI player position
  getPosByUiPos: (uiPos: string) => {
    return get().positionMap?.[uiPos];
  },

  // returns the amount of cards a given player has
  getCardCountByUiPos: (uiPos: string) => {
    const bePos = get().positionMap?.[uiPos];
    const gameState = get().publicState?.gameState;
    if (!bePos || !gameState || gameState.type === "GAME_OVER") return 0;

    let count = gameState.cardCounts[bePos] || 0;

    const pending = get().pendingPlayAreaDisplay;
    if (pending?.type === "TRICK") {
      // publicState is buffered: the player who played the 4th card is off by 1
      const fourthPlayer = Object.keys(pending.cards).at(-1);
      if (bePos === fourthPlayer) count = Math.max(0, count - 1);
    } else if (pending?.type === "TABLEAU") {
      // publicState is buffered: the player who played the last card is off by 1
      if (bePos === pending.lastPlayer) count = Math.max(0, count - 1);
    }

    return count;
  },

  // returns the amount of tricks a given player has
  getTrickCountByUiPos: (uiPos: string) => {
    const bePos = get().positionMap?.[uiPos];
    const gameState = get().publicState?.gameState;
    if (
      bePos &&
      gameState?.type === "CONTRACT_IN_PROGRESS" &&
      gameState.tableState.type === "TRICK"
    ) {
      return gameState.tableState.tricksTaken[bePos] || 0;
    }
    return 0;
  },

  // returns true if the given UI player position is the active player
  getIsActiveByUiPos: (uiPos: string) => {
    const bePos = get().positionMap?.[uiPos];
    const gameState = get().publicState?.gameState;
    if (gameState?.type !== "CONTRACT_IN_PROGRESS") return false;
    return gameState.currentPlayer === bePos;
  },

  // returns player type (BOT or HUMAN) for the given UI player position
  getPlayerTypeByUiPos: (uiPos: string) => {
    const bePos = get().positionMap?.[uiPos];
    if (!bePos) return undefined;

    const player = get().lobbyState?.players.find(
      (player) => player.position === bePos,
    );
    return player?.type;
  },

  // returns sorted hand for the player
  getSortedHand: () => {
    const suitPriority: Record<string, number> = { C: 0, D: 1, S: 2, H: 3 };
    const rankOrder = [
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "J",
      "Q",
      "K",
      "A",
    ];
    const comparator = (a: Card, b: Card) => {
      if (a.suit !== b.suit) return suitPriority[a.suit] - suitPriority[b.suit];
      return rankOrder.indexOf(a.rank) - rankOrder.indexOf(b.rank);
    };

    const pendingPlayArea = get().pendingPlayAreaDisplay;
    if (pendingPlayArea?.type === "TRICK") {
      const myBePos = get().positionMap?.["bottom"];
      const playedCard = myBePos ? pendingPlayArea.cards[myBePos] : null;
      const hand = get().privateState?.hand ?? [];
      const filtered = playedCard
        ? hand.filter(
            (card) =>
              !(card.suit === playedCard.suit && card.rank === playedCard.rank),
          )
        : hand;
      return [...filtered].sort(comparator);
    }
    if (pendingPlayArea?.type === "TABLEAU") {
      const myBePos = get().positionMap?.["bottom"];
      const lastPlayedCard = get().lastPlayedCard;
      const hand = get().privateState?.hand ?? [];
      const filtered =
        myBePos === pendingPlayArea.lastPlayer && lastPlayedCard
          ? hand.filter(
              (card) =>
                !(
                  card.suit === lastPlayedCard.suit &&
                  card.rank === lastPlayedCard.rank
                ),
            )
          : hand;
      return [...filtered].sort(comparator);
    }

    const hand = get().privateState?.hand ?? [];
    return [...hand].sort(comparator);
  },

  checkCanPass: () => {
    const { publicState, privateState, positionMap } = get();
    const myPos = positionMap?.["bottom"];
    const isMyTurn =
      publicState?.gameState?.type === "CONTRACT_IN_PROGRESS" &&
      publicState?.gameState?.currentPlayer === myPos;

    // can pass if it's my turn and I have no legal moves
    return !!(
      isMyTurn &&
      privateState?.legalMoves &&
      privateState.legalMoves.length === 0
    );
  },

  setPopup: (type) => set({ activePopup: type }),

  flushPendingUpdates: () => {
    if (_timer !== null) {
      clearTimeout(_timer);
      _timer = null;
    }
    _waitingForFlush = false;
    set({
      pendingPlayAreaDisplay: null,
      lastPlayedCard: null,
      pendingContractResult: null,
    });
    processNext();
  },

  /*}),
    {
      name: "game-storage",
      partialize: (state) => ({ gameId: state.gameId }),
    },
  ),*/
}));
