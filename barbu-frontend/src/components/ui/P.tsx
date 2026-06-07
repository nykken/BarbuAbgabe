import type { ReactNode } from "react";

export default function P({
  children,
  disabled = false,
  error = false,
  lobby = false,
  bold = false,
  grey = false,
}: {
  children: ReactNode;
  disabled?: boolean;
  error?: boolean;
  lobby?: boolean;
  bold?: boolean;
  grey?: boolean;
}) {
  return (
    <p
      className={`${disabled ? "text-disabled" : error ? "text-red-600" : grey ? "text-gray-400" : "text-text"} ${lobby ? "text-[14px] md:text-[16px]" : "text-[10px] md:text-[12px]"} ${bold ? "font-bold" : "font-normal"} group-hover:text-text-hover m-0 p-0 text-[10px] whitespace-pre-line lg:text-base`}
    >
      {children}
    </p>
  );
}
