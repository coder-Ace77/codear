"""How a problem's answers are compared. The names are the engine's CheckerMode values: keep both in step."""

TOKENS = "TOKENS"                        # word by word, spacing and line breaks ignored, case matters
TOKENS_IGNORE_CASE = "TOKENS_IGNORE_CASE"  # like TOKENS, but "Yes" matches "YES"
EXACT_LINES = "EXACT_LINES"              # line by line: line structure matters, trailing blank space does not
FLOAT = "FLOAT"                          # like TOKENS, but numbers may differ by a tolerance

CHECKER_MODES = (TOKENS, TOKENS_IGNORE_CASE, EXACT_LINES, FLOAT)
DEFAULT_CHECKER = TOKENS
