"use client";

import { VariantsProvider } from "../../apiHelper/VariantsProvider.ts";
import { useEffect, useState } from "react";
import type { Variant } from "../../apiHelper/VariantTypes.ts";
import VariantDetails from "./VariantDetails.tsx";
import P from "../ui/P.tsx";

export default function VariantManager() {
  const isLoading = VariantsProvider((state) => state.isLoading);
  const error = VariantsProvider((state) => state.error);
  const [selectedVariant, setSelectedVariant] = useState<Variant | null>(null);

  const variants = VariantsProvider((state) => state.variants);
  const fetchVariants = VariantsProvider((state) => state.getVariants);

  useEffect(() => {
    //initializes the variants once
    if (variants === null) {
      fetchVariants();
    }
  }, [variants, fetchVariants]);

  if (error)
    return (
      <div>
        <p>Error loading rules: {error}</p>
      </div>
    );

  if (isLoading || !variants) {
    return <div>Loading variants...</div>;
  }

  return (
    <div className="flex size-full flex-col pb-2">
      {/* select variant buttons*/}
      <div className="flex flex-wrap items-center gap-2">
        <P>Variant: </P>
        {variants.map((v) => (
          <button
            key={v.id}
            onClick={() => setSelectedVariant(v)}
            className={`group hover:bg-text hover:border-text block w-fit cursor-pointer rounded-full border px-2 py-1 text-center lg:border-2 ${selectedVariant?.id === v.id ? "bg-menu-rules border-menu-rules-accent" : "border-text"} `}
          >
            <P>{v.displayName}</P>
          </button>
        ))}
      </div>

      {/* rules */}
      <div className="mt-4 min-h-0 flex-1 overflow-auto">
        {selectedVariant && <VariantDetails variant={selectedVariant} />}
      </div>
    </div>
  );
}
