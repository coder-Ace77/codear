"""The choices the problem search accepts. Everything a caller may pick is listed here, so the query builder never
has to trust free text: sort orders and difficulties are looked up, never pasted into SQL."""

DEFAULT_PAGE_SIZE = 10
MAX_PAGE_SIZE = 50

SEARCH_MAX_LENGTH = 100
TAG_MAX_LENGTH = 40
MAX_TAGS = 10

DIFFICULTIES = ("easy", "medium", "hard")
ALL_DIFFICULTIES = "all"  # "no difficulty filter", as the frontend sends it

TAG_MODE_ANY = "any"  # the problem has at least one of the chosen tags
TAG_MODE_ALL = "all"  # the problem has every chosen tag
TAG_MODES = (TAG_MODE_ANY, TAG_MODE_ALL)

# sortBy values. "latest" is the name an older frontend used for "newest".
SORT_RELEVANCE = "relevance"
SORT_POPULARITY = "popularity"
SORT_ACCEPTANCE = "acceptance"
SORT_NEWEST = "newest"
SORT_OLDEST = "oldest"
SORT_TITLE = "title"
SORT_EASIEST = "easiest"
SORT_HARDEST = "hardest"

SORT_OPTIONS = (SORT_RELEVANCE, SORT_POPULARITY, SORT_ACCEPTANCE, SORT_NEWEST, SORT_OLDEST, SORT_TITLE,
                SORT_EASIEST, SORT_HARDEST)
SORT_ALIASES = {"latest": SORT_NEWEST}
DEFAULT_SORT = SORT_RELEVANCE

# How long a cached list of problems may be served. Acceptance and popularity move with every submission.
SEARCH_CACHE_SECONDS = 60
