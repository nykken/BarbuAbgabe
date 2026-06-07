export default function H2({ text }: { text: string }) {
  return (
    <h1 className="text-text m-0 p-0 text-center text-2xl font-bold md:text-4xl lg:text-5xl">
      {text}
    </h1>
  );
}
