import clipboardIcon from "../../assets/clipboard-icon.svg";
import clipboardIconWhite from "../../assets/clipboard-icon-white.svg";

export default function CopyButton({ text }: { text: string }) {
  async function writeClipboardText() {
    try {
      await navigator.clipboard.writeText(text);
    } catch (error) {
      console.error("Failed to copy:", error);
    }
  }

  return (
    <button
      onClick={writeClipboardText}
      className="group border-text hover:bg-text flex h-5 w-5 cursor-pointer items-center justify-center rounded-full border sm:h-6 sm:w-6 md:h-7 md:w-7 lg:h-8 lg:w-8 lg:border-2"
    >
      <img
        src={clipboardIcon}
        alt="clipboard-button"
        className="m-auto block h-4 w-4 group-hover:hidden sm:h-5 sm:w-5 md:h-6 md:w-6 lg:h-7 lg:w-7"
      />

      <img
        src={clipboardIconWhite}
        alt="clipboard-button"
        className="m-auto hidden h-4 w-4 group-hover:block sm:h-5 sm:w-5 md:h-6 md:w-6 lg:h-7 lg:w-7"
      />
    </button>
  );
}
