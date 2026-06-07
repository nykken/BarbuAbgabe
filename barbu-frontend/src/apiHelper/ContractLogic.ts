import { type Contract, SUITS, RANKS, type Scoring } from "./VariantTypes.ts";

export const getContractDescription = (contract: Contract) => {
  const type = contract.type;
  if (type == "REUSSITE") {
    const startingRank =
      RANKS[contract.startingRank as keyof typeof RANKS] ||
      contract.startingRank;
    const replayAfterRank =
      RANKS[contract.replayAfterRank as keyof typeof RANKS] ||
      contract.replayAfterRank;
    const placementPoints = contract.placementPoints;
    return `The first player with zero cards will receive ${placementPoints![0]} Points, the other Players ${placementPoints![1]}, ${placementPoints![2]}, ${placementPoints![3]} Points respectively. ${startingRank == undefined ? "The declarer chooses the starting rank" : "You have to open the turn by playing " + startingRank}. After playing ${replayAfterRank} the player can go again.`;
  } else {
    // "TRICK_TAKING"
    return contract.scoring
      ?.map((scoringPolicy) => getScoringPolicyDescription(scoringPolicy))
      .join("\n");
  }
};

export const getRestriction = (contract: Contract) => {
  const restriction = contract.leadRestriction?.[0];

  const suit = SUITS[restriction?.suit as keyof typeof SUITS];

  switch (restriction?.type) {
    case "OPENING_PHASE": {
      return `You cannot lead ${suit} during the first ${restriction.length} rounds.`;
    }
    case "BROKEN_SUIT": {
      return `After playing a ${suit} card for the first time, ${suit} is allowed to be played.`;
    }
  }
};

export const getContractEndingText = (scoring: Scoring | number) => {
  if (typeof scoring === "number") {
    if (scoring === 0)
      return `All trick taking contract conditions have been met.`; // for reussite
    if (scoring === 4) return `All players have played all their cards.`;
    return `The first ${scoring} players have played all their cards.`;
  }

  switch (scoring.type) {
    case "SUIT": {
      const suit = SUITS[scoring.suit as keyof typeof SUITS];
      return `All ${suit} have been played.`;
    }
    case "RANK": {
      const rank = RANKS[scoring.rank as keyof typeof RANKS] || scoring.rank;
      return `All ${rank}s have been played.`;
    }
    case "CARD": {
      const rank = RANKS[scoring.rank as keyof typeof RANKS] || scoring.rank;
      const suit = SUITS[scoring.suit as keyof typeof SUITS];
      return `The ${rank} of ${suit} has been played.`;
    }
    case "TRICKS": {
      return `All tricks have been played.`;
    }
  }
};

export const getScoringPolicyDescription = (scoring: Scoring) => {
  switch (scoring.type) {
    case "SUIT": {
      const suit = SUITS[scoring.suit as keyof typeof SUITS];
      const points = scoring.pointsPerCard;
      return `Every ${suit} card within a trick gives ${points} Points.`;
    }
    case "TRICKS": {
      const points = scoring.pointsPerTrick;
      return `Every taken trick gives ${points} Points.`;
    }
    case "RANK": {
      const rank = RANKS[scoring.rank as keyof typeof RANKS] || scoring.rank;
      const points = scoring.pointsPerRank;
      return `Every ${rank} card within a trick gives ${points} Points.`;
    }
    case "CARD": {
      const rank = RANKS[scoring.rank as keyof typeof RANKS] || scoring.rank;
      const suit = SUITS[scoring.suit as keyof typeof SUITS];
      const points = scoring.points;
      return `Taking a ${rank} of ${suit} gives ${points} Points.`;
    }
    case "LAST_TWO": {
      const pointsSecondLast = scoring.pointsSecondLast;
      const pointsLast = scoring.pointsLast;
      return `The last trick gives ${pointsLast} Points, the second last trick gives ${pointsSecondLast} Points.`;
    }
  }
};
