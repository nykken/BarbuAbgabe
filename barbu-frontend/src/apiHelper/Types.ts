export const PopupType = {
  Menu: "Menu",
  VariantRules: "Rules",
  GameRules: "Game Rules",
  Scores: "Scores",
} as const;

//creates a Type based on the values above
export type PopupType = (typeof PopupType)[keyof typeof PopupType];

export type Position = "top" | "left" | "right" | "bottom";
