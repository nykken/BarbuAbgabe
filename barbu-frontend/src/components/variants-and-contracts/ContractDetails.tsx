import P from "../ui/P.tsx";
import {
  getContractDescription,
  getRestriction,
} from "../../apiHelper/ContractLogic.ts";
import type { Contract } from "../../apiHelper/VariantTypes.ts";

export default function ContractDetails({ contract }: { contract: Contract }) {
  return (
    <div className="border px-2 py-1 lg:px-3 lg:py-2">
      <P>
        <strong>{contract.displayName}</strong>
      </P>
      <P>{getContractDescription(contract)}</P>
      {contract.leadRestriction && contract.leadRestriction.length > 0 && (
        <div className="">
          <P>
            Restriction: <span>{getRestriction(contract)}</span>
          </P>
        </div>
      )}
    </div>
  );
}
