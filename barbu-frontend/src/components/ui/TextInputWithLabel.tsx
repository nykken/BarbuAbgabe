import P from "./P.tsx";

export default function TextInputWithLabel({
  labelText,
  id,
  password = false,
  minLength,
  required = false,
}: {
  labelText: string;
  id: string;
  password?: boolean;
  minLength?: number;
  required?: boolean;
}) {
  return (
    <div className="w-full">
      <label htmlFor={id} className="m-0">
        <P> {labelText} </P>
      </label>
      <input
        id={id}
        name={id}
        type={password ? "password" : "text"}
        className="border-text focus:bg-text/8 m-0 w-full rounded-sm border px-2 py-1 text-xs focus:outline-none lg:my-1 lg:text-base lg:md:text-sm xl:px-3 xl:py-2"
        minLength={minLength}
        required={required}
      />
    </div>
  );
}
