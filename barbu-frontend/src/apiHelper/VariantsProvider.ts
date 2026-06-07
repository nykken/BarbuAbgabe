import { create } from "zustand";
import type { Variant } from "./VariantTypes.ts";

const host = import.meta.env.VITE_API_BASE_URL || "localhost:8080";

export const VariantsProvider = create<{
  variants: Variant[] | null;
  isLoading: boolean;
  error: string | null;
  getVariants: () => Promise<Variant[] | null>;
}>((set, get) => ({
  variants: null,
  isLoading: false,
  error: null,

  //fetch all variants
  getVariants: async () => {
    if (!get().variants && !get().error && !get().isLoading) {
      set({ isLoading: true, error: null });
      try {
        const response = await fetch(`http://${host}/api/variants`);
        if (response.ok) {
          const data: Variant[] = await response.json();
          set({ variants: data, isLoading: false });
          return data;
        } else {
          const errorText = await response.text();
          set({
            error: errorText || "Could not get game variants.",
            isLoading: false,
          });
        }
      } catch (error: any) {
        set({
          error: "Cannot connect to server. Please check your connection.",
          isLoading: false,
        });
      }
    }
    return null;
  },
}));
