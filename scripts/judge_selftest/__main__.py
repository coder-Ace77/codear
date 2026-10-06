"""Submits a set of hostile and broken programs to the live judge and checks each gets the right verdict.

    python3 -m scripts.judge_selftest                       (run from the repo root)
    python3 -m scripts.judge_selftest --only memory --problem 10
    python3 -m scripts.judge_selftest --list

Uses API_KEY from .env. Every case is a real submission. An admin key may submit back to back; a normal user's
key needs --delay 10.
"""
import argparse
import sys
import time

from scripts.codear_api import ApiClient, load_settings
from scripts.judge_selftest.cases import CASES


def run_case(client, problem_id, case):
    """(passed, one-line explanation)"""
    submission_id = client.submit(problem_id, case.language, case.code)
    final = client.watch(submission_id)
    result = final.get("result") or {}
    verdict, message = result.get("verdict"), result.get("result") or ""

    if verdict != case.verdict:
        return False, f"expected {case.verdict}, got {verdict}: {message[:90]!r}"
    if case.message_has and case.message_has.lower() not in message.lower():
        return False, f"message lacks {case.message_has!r}: {message[:90]!r}"
    if case.message_lacks and case.message_lacks in message:
        return False, f"message leaks {case.message_lacks!r}: {message[:90]!r}"
    return True, f"{verdict}" + (f" on test {result.get('failedTest')}" if result.get("failedTest") else "")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--problem", type=int, default=10, help="problem id (default 10, Multiply)")
    parser.add_argument("--only", default="", help="run only cases whose name contains this")
    parser.add_argument("--delay", type=float, default=0, help="seconds to wait between submissions")
    parser.add_argument("--list", action="store_true", help="list the cases and exit")
    args = parser.parse_args()

    cases = [c for c in CASES if args.only.lower() in c.name.lower()]
    if args.list:
        for case in cases:
            print(f"{case.verdict:<22} {case.name}")
        return 0

    base, key = load_settings()
    client = ApiClient(base, key)
    print(f"Judge self-test: {len(cases)} programs against problem {args.problem} at {base}\n")

    failures = 0
    started = time.monotonic()
    for case in cases:
        t0 = time.monotonic()
        try:
            passed, detail = run_case(client, args.problem, case)
        except RuntimeError as e:
            passed, detail = False, str(e)
        failures += not passed
        print(f"{'PASS' if passed else 'FAIL'}  {case.name:<46} {time.monotonic() - t0:5.1f}s  {detail}", flush=True)
        if args.delay:
            time.sleep(args.delay)

    print(f"\n{len(cases) - failures}/{len(cases)} behaved correctly in {time.monotonic() - started:.0f}s")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
