import type { ReactNode } from "react";
import H2 from "../ui/H2.tsx";
import ReturnButton from "../ui/ReturnButton.tsx";

interface CustomProps {
  children: ReactNode;
  title: string;
  home?: boolean;
  login?: boolean;
  onReturn?: () => void;
}

export default function StartPagesLayout({
  children,
  title,
  home = false,
  login = false,
}: CustomProps) {
  return (
    <div className="relative flex h-full flex-col">
      {!home && !login && (
        <div className="flex w-full flex-1 items-end justify-center">
          <H2 text={title} />
        </div>
      )}
      {!home && !login && (
        <div className="absolute top-0 left-0">
          <ReturnButton />
        </div>
      )}
      <div className="flex min-h-0 flex-5 items-center justify-center">
        {children}
      </div>
    </div>
  );
}
