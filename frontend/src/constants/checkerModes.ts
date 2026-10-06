/** How a problem's answers are compared. The values are the backend's checker names. */
export interface CheckerOption {
  value: string;
  label: string;
  hint: string;
}

export const CHECKER_OPTIONS: CheckerOption[] = [
  { value: "TOKENS", label: "Ignore spacing", hint: "Word by word. Spaces and line breaks do not matter; capital letters do." },
  { value: "TOKENS_IGNORE_CASE", label: "Ignore spacing and case", hint: "Like the above, and \"Yes\" matches \"YES\"." },
  { value: "EXACT_LINES", label: "Line by line", hint: "Each line must match. Trailing spaces and blank lines are ignored." },
  { value: "FLOAT", label: "Numbers within a tolerance", hint: "Like the first, but numbers may differ by a small amount." },
];

export const DEFAULT_CHECKER = "TOKENS";
export const DEFAULT_FLOAT_TOLERANCE = 0.000001;
