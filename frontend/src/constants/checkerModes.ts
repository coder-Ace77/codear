/** How a problem's answers are compared. The values are the backend's checker names. */
export interface CheckerOption {
  value: string;
  label: string;
  hint: string;
}

export const CHECKER_OPTIONS: CheckerOption[] = [
  { value: "TOKENS", label: "Ignore spacing", hint: "Word by word. Spaces and line breaks do not matter; capital letters do." },
  { value: "FLOAT", label: "Numbers within a tolerance", hint: "Like the first, but numbers may differ by a small amount." },
];

export const DEFAULT_CHECKER = "TOKENS";
export const DEFAULT_FLOAT_TOLERANCE = 0.000001;
