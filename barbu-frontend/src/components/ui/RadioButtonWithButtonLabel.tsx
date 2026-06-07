import P from "./P.tsx";

export default function RadioButtonWithButtonLabel({
  text,
  id,
  groupName,
  disabled = false,
  small = false,
  noHover = false,
  onChange,
  defaultChecked,
}: {
  text: string;
  id: string;
  groupName: string;
  disabled?: boolean;
  small?: boolean;
  noHover?: boolean;
  onChange?: () => void;
  defaultChecked?: boolean;
}) {
  return (
    <div>
      <input
        type="radio"
        name={groupName}
        id={id}
        value={id}
        className="peer hidden"
        required
        onChange={onChange}
        defaultChecked={defaultChecked}
      />
      <label
        htmlFor={id}
        className={`block w-full rounded-full border ${small ? "px-1 py-0.5 md:px-2 md:py-1" : "px-2 py-1"} text-center lg:border-2 ${disabled ? "border-disabled bg-disabled/10" : "border-text"} ${!noHover && "hover:bg-menu-rules peer-checked:bg-menu-rules peer-checked:border-menu-rules-accent cursor-pointer"}`}
      >
        <P disabled={disabled}>{text}</P>
      </label>
    </div>
  );
}