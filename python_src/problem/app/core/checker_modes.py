"""How a problem's answers are compared. The engine's ComparatorFactory reads these names: keep both in step."""

TOKENS = "TOKENS"  # word by word, spacing and line breaks ignored, case matters
FLOAT = "FLOAT"    # like TOKENS, but numbers may differ by a tolerance

CHECKER_MODES = (TOKENS, FLOAT)
DEFAULT_CHECKER = TOKENS
