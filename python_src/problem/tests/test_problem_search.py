import pytest

from app.core.search_options import MAX_PAGE_SIZE, MAX_TAGS, SEARCH_MAX_LENGTH, SORT_OPTIONS
from app.database import SessionLocal, redis_client
from app.models.problem import Submission, SubmissionStatus
from tests.helpers import USER_ID, OTHER_USER_ID

SEARCH = "/api/v1/problem/search"
TAGS = "/api/v1/problem/tags"


@pytest.fixture
def library(add_problem):
    """Five problems, added in this order, with statistics on three of them. Returns {title: id}."""
    ids = {
        "Balanced Brackets": add_problem(
            title="Balanced Brackets", difficulty="Easy", tags=["Stack", "String"],
            description="Check that every bracket is closed in the right order."),
        "Shortest Path in a Grid": add_problem(
            title="Shortest Path in a Grid", difficulty="Medium", tags=["Graph", "BFS"],
            description="Find the shortest path between two cells of a grid."),
        "Edit Distance": add_problem(
            title="Edit Distance", difficulty="Hard", tags=["Dynamic Programming", "String"],
            description="Minimum operations to turn one word into another."),
        "Two Sum": add_problem(
            title="Two Sum", difficulty="Easy", tags=["array", "Hash Table"],  # lower case on purpose
            description="Find two numbers that add up to a target."),
        "100% Fun_Run": add_problem(
            title="100% Fun_Run", difficulty="Medium", tags=[],
            description="Literal percent and underscore characters appear in this title."),
    }
    # Balanced Brackets: 3 of 4 accepted, by two people. Two Sum: 1 of 2. Shortest Path: 0 of 1.
    submit(ids["Balanced Brackets"], [(USER_ID, True), (USER_ID, True), (OTHER_USER_ID, True), (OTHER_USER_ID, False)])
    submit(ids["Two Sum"], [(USER_ID, True), (USER_ID, False)])
    submit(ids["Shortest Path in a Grid"], [(USER_ID, False)])
    return ids


def submit(problem_id, outcomes):
    session = SessionLocal()
    try:
        for n, (user_id, accepted) in enumerate(outcomes):
            session.add(Submission(
                submission_id=f"s-{problem_id}-{n}", user_id=user_id, problem_id=problem_id, code="x",
                language="python", status=SubmissionStatus.PASSED if accepted else SubmissionStatus.FAILED))
        session.commit()
    finally:
        session.close()


def find(client, **params):
    response = client.get(SEARCH, params=params)
    assert response.status_code == 200, response.text
    return response.json()


def titles(body):
    return [p["title"] for p in body["content"]]


# ---------------------------------------------------------------- searching words

def test_a_title_prefix_finds_the_problem(client, library):
    assert titles(find(client, search="bal")) == ["Balanced Brackets"]


def test_a_word_from_the_middle_of_a_title_finds_it(client, library):
    assert titles(find(client, search="path")) == ["Shortest Path in a Grid"]


def test_search_ignores_capitals(client, library):
    assert titles(find(client, search="EDIT dist")) == ["Edit Distance"]


def test_a_word_only_in_the_statement_finds_the_problem(client, library):
    assert titles(find(client, search="underscore")) == ["100% Fun_Run"]
    assert titles(find(client, search="cells")) == ["Shortest Path in a Grid"]


def test_the_statement_matches_other_forms_of_a_word(client, library):
    # "balance" is not in any text as written, but the statement says "closed" and the title "Balanced"
    assert titles(find(client, search="balance")) == ["Balanced Brackets"]


def test_a_tag_is_searchable_as_a_word(client, library):
    assert titles(find(client, search="graph")) == ["Shortest Path in a Grid"]
    assert titles(find(client, search="dynamic")) == ["Edit Distance"]


def test_percent_and_underscore_are_ordinary_characters(client, library):
    assert titles(find(client, search="100%")) == ["100% Fun_Run"]
    assert titles(find(client, search="_run")) == ["100% Fun_Run"]
    assert find(client, search="%")["totalCount"] == 1          # only the title that really contains a percent sign
    assert find(client, search="_")["totalCount"] == 1           # "_" is not "any character": only the real underscore


def test_nothing_matching_gives_an_empty_page(client, library):
    body = find(client, search="zzzzzz")

    assert body == {"content": [], "totalCount": 0, "totalPages": 0}


def test_hostile_text_is_just_text(client, library):
    for nasty in ["'; DROP TABLE problems; --", "\\", "a\\", "%%%", "' OR '1'='1"]:
        find(client, search=nasty)
    assert find(client)["totalCount"] == 5


def test_leading_and_trailing_spaces_in_the_search_are_ignored(client, library):
    assert titles(find(client, search="  two sum  ")) == ["Two Sum"]


# ---------------------------------------------------------------- relevance

def test_best_matches_come_first_when_searching(client, add_problem):
    add_problem(title="Parser", tags=["Brackets"], description="Tokenise some text.")
    add_problem(title="Stack", description="Watch the brackets in the text.")
    add_problem(title="Balanced Brackets", description="Nested things.")
    add_problem(title="Brackets Game", description="A game.")

    # title starts with it, title contains it, a tag has it, only the statement has it
    assert titles(find(client, search="brackets")) == ["Brackets Game", "Balanced Brackets", "Parser", "Stack"]


def test_without_a_search_the_default_order_is_the_order_problems_were_added(client, library):
    assert titles(find(client)) == ["Balanced Brackets", "Shortest Path in a Grid", "Edit Distance", "Two Sum", "100% Fun_Run"]


# ---------------------------------------------------------------- difficulty

def test_filtering_by_difficulty(client, library):
    assert titles(find(client, difficulty="easy")) == ["Balanced Brackets", "Two Sum"]
    assert titles(find(client, difficulty="hard")) == ["Edit Distance"]


def test_difficulty_ignores_capitals_and_all_means_no_filter(client, library):
    assert titles(find(client, difficulty="MeDiUm")) == ["Shortest Path in a Grid", "100% Fun_Run"]
    assert find(client, difficulty="all")["totalCount"] == 5
    assert find(client, difficulty="")["totalCount"] == 5


# ---------------------------------------------------------------- tags

def test_filtering_by_one_tag(client, library):
    assert titles(find(client, tags=["String"])) == ["Balanced Brackets", "Edit Distance"]


def test_tags_ignore_capitals_in_both_the_request_and_the_data(client, library):
    assert titles(find(client, tags=["STRING"])) == ["Balanced Brackets", "Edit Distance"]
    assert titles(find(client, tags=["Array"])) == ["Two Sum"]       # stored as "array"
    assert titles(find(client, tags=["ARRAY"])) == ["Two Sum"]


def test_several_tags_match_any_by_default(client, library):
    assert titles(find(client, tags=["graph", "hash table"])) == ["Shortest Path in a Grid", "Two Sum"]


def test_all_mode_needs_every_tag(client, library):
    assert titles(find(client, tags=["string", "stack"], tagMode="all")) == ["Balanced Brackets"]
    assert titles(find(client, tags=["string", "graph"], tagMode="all")) == []


def test_repeating_a_tag_does_not_break_all_mode(client, library):
    assert titles(find(client, tags=["string", "STRING", "string"], tagMode="all")) == ["Balanced Brackets", "Edit Distance"]


def test_a_tag_that_nothing_has_matches_nothing(client, library):
    assert find(client, tags=["nope"])["totalCount"] == 0


def test_problems_without_tags_are_never_matched_by_a_tag(client, library):
    assert "100% Fun_Run" not in titles(find(client, tags=["string", "graph", "stack", "array"]))


# ---------------------------------------------------------------- combining filters

def test_search_difficulty_and_tags_all_apply_together(client, library):
    assert titles(find(client, search="string", difficulty="easy", tags=["stack"])) == ["Balanced Brackets"]
    assert find(client, search="string", difficulty="hard", tags=["stack"])["totalCount"] == 0


# ---------------------------------------------------------------- sorting

def test_sorting_newest_and_oldest(client, library):
    assert titles(find(client, sortBy="newest"))[0] == "100% Fun_Run"
    assert titles(find(client, sortBy="oldest"))[0] == "Balanced Brackets"
    assert titles(find(client, sortBy="latest"))[0] == "100% Fun_Run"        # the old name still works


def test_sorting_by_title_ignores_capitals(client, add_problem):
    for title in ["banana", "Cherry", "apple"]:
        add_problem(title=title)

    assert titles(find(client, sortBy="title")) == ["apple", "banana", "Cherry"]


def test_sorting_by_difficulty(client, library):
    assert [p["difficulty"] for p in find(client, sortBy="easiest")["content"]] == ["Easy", "Easy", "Medium", "Medium", "Hard"]
    assert [p["difficulty"] for p in find(client, sortBy="hardest")["content"]] == ["Hard", "Medium", "Medium", "Easy", "Easy"]


def test_sorting_by_popularity_uses_how_many_people_solved_it(client, library):
    # Balanced Brackets: 2 solvers, Two Sum: 1, the rest none (ties go to the one with more submissions, then the older)
    assert titles(find(client, sortBy="popularity"))[:3] == ["Balanced Brackets", "Two Sum", "Shortest Path in a Grid"]


def test_sorting_by_acceptance_puts_problems_nobody_tried_last(client, library):
    assert titles(find(client, sortBy="acceptance")) == [
        "Balanced Brackets",          # 75%
        "Two Sum",                    # 50%
        "Shortest Path in a Grid",    # 0%, tried
        "Edit Distance",              # never tried
        "100% Fun_Run",
    ]


@pytest.mark.parametrize("sort", SORT_OPTIONS)
def test_every_sort_returns_every_problem_exactly_once(client, library, sort):
    body = find(client, sortBy=sort, size=50)

    assert sorted(titles(body)) == sorted(library)


# ---------------------------------------------------------------- the numbers in each row

def test_each_row_carries_its_statistics(client, library):
    by_title = {p["title"]: p for p in find(client)["content"]}

    assert by_title["Balanced Brackets"]["acceptance"] == 75.0
    assert by_title["Balanced Brackets"]["submissions"] == 4
    assert by_title["Balanced Brackets"]["solvedBy"] == 2
    assert by_title["Two Sum"]["acceptance"] == 50.0
    assert by_title["Shortest Path in a Grid"]["acceptance"] == 0.0     # tried and never solved
    assert by_title["Edit Distance"]["acceptance"] is None              # nobody has tried it
    assert by_title["Edit Distance"]["submissions"] == 0


def test_acceptance_is_rounded_to_one_decimal(client, add_problem):
    problem = add_problem(title="Thirds")
    submit(problem, [(USER_ID, True), (USER_ID, False), (USER_ID, False)])

    assert find(client)["content"][0]["acceptance"] == 33.3


def test_a_row_keeps_the_fields_the_frontend_already_reads(client, library):
    row = find(client)["content"][0]

    assert {"id", "title", "tags", "difficulty"} <= set(row)
    assert row["tags"] == ["Stack", "String"]


# ---------------------------------------------------------------- paging

def test_totals_describe_all_matches_not_just_the_page(client, library):
    body = find(client, size=2)

    assert len(body["content"]) == 2
    assert body["totalCount"] == 5
    assert body["totalPages"] == 3


def test_the_last_page_is_shorter(client, library):
    assert len(find(client, size=2, page=2)["content"]) == 1


def test_a_page_past_the_end_is_empty_but_still_knows_the_total(client, library):
    body = find(client, size=2, page=9)

    assert body["content"] == []
    assert body["totalCount"] == 5


def test_totals_follow_the_filters(client, library):
    body = find(client, difficulty="easy", size=1)

    assert (body["totalCount"], body["totalPages"], len(body["content"])) == (2, 2, 1)


def test_paging_keeps_the_chosen_sort(client, library):
    first = titles(find(client, sortBy="title", size=2, page=0))
    second = titles(find(client, sortBy="title", size=2, page=1))

    assert first + second == ["100% Fun_Run", "Balanced Brackets", "Edit Distance", "Shortest Path in a Grid"]


# ---------------------------------------------------------------- bad input

@pytest.mark.parametrize("params", [
    {"size": 0}, {"size": MAX_PAGE_SIZE + 1}, {"page": -1}, {"sortBy": "chaos"}, {"difficulty": "impossible"},
    {"tagMode": "some"}, {"search": "x" * (SEARCH_MAX_LENGTH + 1)}, {"tags": ["t" * 41]},
    {"tags": [f"tag{i}" for i in range(MAX_TAGS + 1)]},
])
def test_invalid_requests_are_rejected_with_a_reason(client, library, params):
    response = client.get(SEARCH, params=params)

    assert response.status_code == 422


def test_the_largest_allowed_page_and_the_most_tags_are_accepted(client, library):
    assert client.get(SEARCH, params={"size": MAX_PAGE_SIZE}).status_code == 200
    assert client.get(SEARCH, params={"tags": [f"tag{i}" for i in range(MAX_TAGS)]}).status_code == 200


def test_empty_tag_values_are_ignored(client, library):
    assert find(client, tags=["", "  "])["totalCount"] == 5


# ---------------------------------------------------------------- the tag list

def test_the_tag_list_counts_problems_and_merges_capitals(client, library):
    counts = {t["tag"]: t["count"] for t in client.get(TAGS).json()}

    assert counts["String"] == 2
    assert counts["Stack"] == 1
    assert counts["array"] == 1
    assert len(counts) == 7


def test_the_tag_list_is_most_used_first(client, library):
    counts = client.get(TAGS).json()

    assert counts[0] == {"tag": "String", "count": 2}
    assert [c["count"] for c in counts] == sorted((c["count"] for c in counts), reverse=True)


def test_two_spellings_of_a_tag_count_as_one(client, add_problem):
    add_problem(title="a", tags=["Graph"])
    add_problem(title="b", tags=["graph"])

    assert [t["count"] for t in client.get(TAGS).json()] == [2]


def test_no_problems_means_no_tags(client):
    assert client.get(TAGS).json() == []


# ---------------------------------------------------------------- caching

def test_plain_browsing_is_cached_but_free_text_searches_are_not(client, library):
    find(client, search="brackets")
    assert redis_client.keys("Search_problem:v2:*") == []

    find(client, difficulty="easy")
    assert len(redis_client.keys("Search_problem:v2:*")) == 1


def test_a_new_problem_shows_up_in_a_cached_list(client, library, add_problem):
    assert find(client)["totalCount"] == 5       # fills the cache

    add_problem(title="Brand New")

    body = find(client)
    assert body["totalCount"] == 6
    assert "Brand New" in titles(body)


def test_different_sorts_and_tag_modes_do_not_share_a_cache_entry(client, library):
    assert titles(find(client, sortBy="title"))[0] == "100% Fun_Run"
    assert titles(find(client, sortBy="newest"))[0] == "100% Fun_Run"
    assert titles(find(client, sortBy="oldest"))[0] == "Balanced Brackets"
    assert titles(find(client, tags=["string", "stack"], tagMode="all")) == ["Balanced Brackets"]
    assert titles(find(client, tags=["string", "stack"], tagMode="any")) == ["Balanced Brackets", "Edit Distance"]
