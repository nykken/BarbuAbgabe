import StartPagesLayout from "../components/start-pages/StartPagesLayout.tsx";
import VariantManager from "../components/variants-and-contracts/VariantManager.tsx";

export default function Ruleset() {
  return (
    <StartPagesLayout title="Game Rules">
      <div className="size-full px-2 pt-4">
        <VariantManager />
      </div>
    </StartPagesLayout>
  );
}
