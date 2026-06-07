import type { ReactNode } from "react";

interface CustomProps {
  children: ReactNode;
}

export default function GameContainer({ children }: CustomProps) {
  return (
    <div className="flex h-screen w-screen items-center justify-center px-10">
      <div className="bg-gameboard relative aspect-video w-full max-w-[calc(100vh*16/9)] overflow-hidden p-1 lg:p-2 xl:w-7xl">
        {children}
      </div>
    </div>
  );
}
