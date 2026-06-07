import {
  RANKING_ORDERS,
  type Variant,
  DECK_NAMES,
} from "../../apiHelper/VariantTypes.ts";
import P from "../ui/P.tsx";
import H3 from "../ui/H3.tsx";
import ContractDetails from "./ContractDetails.tsx";

export default function VariantDetails({ variant }: { variant: Variant }) {
  return (
    <div className="flex flex-col gap-2">
      <H3 text={variant.displayName} />

      {/* core game principle */}
      <div className="rounded-md bg-black/10 px-2 py-1">
        <P>
          <strong>Game Principle</strong>
        </P>
        <P>
          The game consists of <strong>4 rounds</strong>. During each round,
          every player gets to be the
          <strong> Declarer</strong> once. The Declarer chooses a{" "}
          <strong>Contract</strong> (a mini-game with unique scoring) from their
          available pool. Each contract can only be played once per player.
        </P>
      </div>

      {/* trick/reussite  rules */}
      <div className="flex gap-2">
        <div className="rounded-md bg-black/10 px-2 py-1">
          <P>
            <strong>Trick taking contracts: </strong> <br />
            <span>
              Players must follow the suit of the starting card if possible. The
              highest card of the led suit wins the trick. Depending on the win
              condition, your goal is either to <strong>avoid</strong> taking
              specific tricks/cards, or to <strong>win</strong> as many as
              possible.
            </span>
          </P>
        </div>

        <div className="rounded-md bg-black/10 px-2 py-1">
          <P>
            <strong>Réussite contracts: </strong> <br />
            <span>
              A domino-style layout game. Instead of playing tricks, players
              take turns adding cards to sequential suit-piles on the table
              (starting from a designated card and building outwards). The
              objective is to
              <strong> discard your entire hand</strong> first.
            </span>
          </P>
        </div>
      </div>

      <div className="mb-2 flex flex-col justify-evenly lg:mt-3 lg:mb-6">
        <P>
          <strong>Deck: </strong>
          {DECK_NAMES[variant.deckVariant] || variant.deckVariant}
        </P>
        <P>
          <strong>Win condition: </strong>
          {RANKING_ORDERS[variant.rankingOrder] || variant.rankingOrder}
        </P>
      </div>

      <div className="grid grid-cols-2 gap-1 md:gap-2 xl:gap-4">
        {variant.contracts.map((contract) => (
          <ContractDetails key={contract.id} contract={contract} />
        ))}
      </div>
    </div>
  );
}
