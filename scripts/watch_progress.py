#!/usr/bin/env python3
"""Submit a solution and watch it being judged, update by update.

    python3 scripts/watch_progress.py <problem-id> <cpp|python> <source file>

Uses API_KEY from .env and the API address from frontend/.env. Prints every progress update the long-poll
delivers, with the time since submitting, then a summary of how smooth the updates were.
"""
import sys
import time

from codear_api import ApiClient, load_settings


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    problem_id, language, path = int(sys.argv[1]), sys.argv[2], sys.argv[3]
    client = ApiClient(*load_settings())

    started = time.monotonic()
    submission_id = client.submit(problem_id, language, open(path).read())
    print(f"submitted {submission_id[:8]} in {time.monotonic() - started:.2f}s\n")
    print(f"{'time':>7}  {'ver':>3}  {'stage':<10} {'%':>4}  message")

    updates, last = [], started

    def show(update):
        nonlocal last
        now = time.monotonic()
        updates.append((now - started, now - last))
        last = now
        print(f"{now - started:6.2f}s  {update['version']:>3}  {update['stage']:<10} {update['percent']:>3}%  {update['message']}")

    final = client.watch(submission_id, on_update=show)

    result = final.get("result") or {}
    print()
    print(f"verdict {result.get('verdict') or final['status']}  tests {result.get('passedTests')}/{result.get('totalTests')}"
          f"  judge cpu {result.get('timeTakenMs')} ms  {result.get('memoryUsed')}")
    if result.get("result") and result.get("verdict") != "ACCEPTED":
        print(result["result"])
    gaps = [g for _, g in updates[1:]]
    print(f"{len(updates)} updates in {updates[-1][0]:.1f}s" + (f"; longest wait between updates {max(gaps):.2f}s" if gaps else ""))


if __name__ == "__main__":
    main()
