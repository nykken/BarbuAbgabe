export default function H3({ text }: { text: string }) {
  return (
    <h1 className="text-text m-0 p-0 text-sm font-bold md:text-xl lg:text-3xl">
      {text}
    </h1>
  );
}
