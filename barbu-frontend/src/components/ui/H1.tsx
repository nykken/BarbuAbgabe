export default function H1({ text }: { text: string }) {
  return (
    <h1 className="text-text m-0 p-0 text-center text-6xl font-bold md:text-8xl lg:text-9xl">
      {text}
    </h1>
  );
}
