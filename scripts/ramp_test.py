#!/usr/bin/env python3
"""Ramp test for the submission API, using an API key.

    python3 scripts/ramp_test.py

Phase 1: 1 request every 2 seconds for 30 seconds (15 requests).
Phase 2: 1 request every 1 second for 30 seconds (30 requests).
Then it waits for the verdicts of everything that was accepted.

Reads API_KEY from .env (repo root), and the API address from API_BASE in .env or VITE_API_BASE in
frontend/.env. Standard library only. Every request becomes a real submission that the judge runs,
so the default problem is a tiny one.

What to expect: a regular user is allowed one submission per 10 seconds, so most requests should
come back 429 with a Retry-After header, and accepted ones should be at least 10 seconds apart.
An admin's key is allowed 100 per second, so every request should be accepted.
"""
import argparse
import concurrent.futures as cf
import json
import os
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SOLUTIONS = {
    # Problem 10, "Multiply": read two numbers, print their product.
    "python": "a, b = map(int, input().split())\nprint(a * b)\n",
    "cpp": "#include <iostream>\nint main(){long long a,b;std::cin>>a>>b;std::cout<<a*b<<std::endl;}\n",
}


def read_env(path):
    values = {}
    try:
        with open(path) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    values[k.strip()] = v.strip().strip("'\"")
    except FileNotFoundError:
        pass
    return values


def now_label(t0):
    return f"{time.monotonic() - t0:5.1f}s"


class Recorder:
    def __init__(self):
        self.lock = threading.Lock()
        self.rows = []  # dicts, one per submit request

    def say(self, text):
        with self.lock:
            print(text, flush=True)

    def add(self, row):
        with self.lock:
            self.rows.append(row)


def call(method, url, key, body=None, timeout=30):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("X-API-Key", key)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode()
            return resp.status, dict(resp.headers), raw, time.monotonic() - started
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode(), time.monotonic() - started
    except Exception as e:  # network failure, timeout
        return 0, {}, str(e), time.monotonic() - started


def short(raw, n=110):
    try:
        d = json.loads(raw)
        if isinstance(d, dict) and "detail" in d:
            raw = d["detail"] if isinstance(d["detail"], str) else json.dumps(d["detail"])
    except Exception:
        pass
    raw = " ".join(str(raw).split())
    return raw if len(raw) <= n else raw[: n - 1] + "…"


def fire(n, phase, t0, rec, base, key, problem, language):
    sent_at = time.monotonic() - t0
    status, headers, raw, took = call(
        "POST", f"{base}/problem/submit", key,
        {"problemId": problem, "language": language, "code": SOLUTIONS[language]},
    )
    retry = headers.get("Retry-After") or headers.get("retry-after")
    sub_id = None
    if status == 200:
        try:
            sub_id = json.loads(raw)["submissionId"]
        except Exception:
            pass
    row = {"n": n, "phase": phase, "sent_at": sent_at, "status": status, "took": took, "retry_after": retry, "id": sub_id}
    rec.add(row)

    if status == 200:
        verdict = f"ACCEPTED  submissionId={sub_id[:8] if sub_id else '?'}"
    elif status == 429:
        verdict = f"RATE LIMITED  Retry-After={retry}s  \"{short(raw)}\""
    elif status == 0:
        verdict = f"NETWORK ERROR  {short(raw)}"
    else:
        verdict = f"ERROR  \"{short(raw)}\""
    rec.say(f"[{sent_at:5.1f}s] #{n:02d} phase {phase}  POST /problem/submit -> {status or '---'} in {took * 1000:6.0f} ms   {verdict}")


def poll(row, t0, rec, base, key):
    """Long-polls one accepted submission until it leaves IN_PROGRESS (each call waits up to 10 s)."""
    started = time.monotonic()
    body = {}
    for _ in range(12):
        status, _, raw, _ = call("GET", f"{base}/problem/submissions/{row['id']}", key, timeout=40)
        if status != 200:
            rec.say(f"        #{row['n']:02d} result poll -> {status}  \"{short(raw)}\"")
            row["verdict"] = f"HTTP {status}"
            return
        body = json.loads(raw)
        if body.get("status") != "IN_PROGRESS":
            break
    row["verdict"] = body.get("status", "?")
    row["judge_wait"] = time.monotonic() - started
    rec.say(
        f"        #{row['n']:02d} {row['id'][:8]} -> {body.get('status')}  "
        f"tests {body.get('passedTests')}/{body.get('totalTests')}  time {body.get('timeTakenMs')} ms  "
        f"(waited {time.monotonic() - started:4.1f}s for the verdict)"
    )


def pct(values, p):
    values = sorted(values)
    return values[min(len(values) - 1, int(round(p / 100 * (len(values) - 1))))]


def summarise(rec, t0):
    rows = sorted(rec.rows, key=lambda r: r["n"])
    print("\n" + "=" * 78)
    print("SUMMARY")
    print("=" * 78)
    if not rows:
        print("No requests were sent.")
        return

    ok = [r for r in rows if r["status"] == 200]
    limited = [r for r in rows if r["status"] == 429]
    other = [r for r in rows if r["status"] not in (200, 429)]
    print(f"Requests sent      : {len(rows)}")
    print(f"  accepted (200)   : {len(ok)}")
    print(f"  rate limited(429): {len(limited)}")
    print(f"  other / failed   : {len(other)}" + (f"   statuses: {sorted({r['status'] for r in other})}" if other else ""))

    for phase, label in ((1, "phase 1 (1 per 2s)"), (2, "phase 2 (1 per 1s)")):
        pr = [r for r in rows if r["phase"] == phase]
        if pr:
            print(f"  {label:<20}: {sum(r['status'] == 200 for r in pr)} accepted, {sum(r['status'] == 429 for r in pr)} limited, of {len(pr)}")

    took = [r["took"] * 1000 for r in rows if r["status"] in (200, 429)]
    if took:
        print(f"Response time (ms) : min {min(took):.0f}   median {statistics.median(took):.0f}   p95 {pct(took, 95):.0f}   max {max(took):.0f}")

    if len(ok) >= 2:
        times = sorted(r["sent_at"] for r in ok)
        gaps = [b - a for a, b in zip(times, times[1:])]
        print(f"Gaps between accepted submissions: min {min(gaps):.1f}s   median {statistics.median(gaps):.1f}s   max {max(gaps):.1f}s")

    # Is Retry-After honest? It should be about (10 - time since the last accepted submission).
    checks = []
    for r in limited:
        last_ok = [a["sent_at"] for a in ok if a["sent_at"] <= r["sent_at"]]
        if last_ok and r["retry_after"]:
            expected = 10 - (r["sent_at"] - max(last_ok))
            checks.append(abs(float(r["retry_after"]) - expected))
    if checks:
        print(f"Retry-After accuracy: off by {statistics.mean(checks):.1f}s on average (max {max(checks):.1f}s)")

    verdicts = {}
    for r in ok:
        verdicts[r.get("verdict", "not checked")] = verdicts.get(r.get("verdict", "not checked"), 0) + 1
    if verdicts:
        print("Verdicts           : " + ", ".join(f"{k} x{v}" for k, v in verdicts.items()))

    print("\nREADING THE RESULT")
    if not other and ok and limited:
        gaps_ok = len(ok) < 2 or min(b - a for a, b in zip(sorted(r['sent_at'] for r in ok), sorted(r['sent_at'] for r in ok)[1:])) >= 9.0
        print("  PASS: the per-user limit is working." if gaps_ok else "  CHECK: some accepted submissions were closer than 10s apart.")
        print(f"        Expected about {int(60 // 10) + 1} accepted in 60s for a regular user; got {len(ok)}.")
    elif not other and ok and not limited:
        print("  Nothing was rate limited. That is right for an ADMIN key (100/s) but means the 10s")
        print("  user limit did not apply. If this key belongs to a regular user, the limiter is not working.")
    elif other:
        print("  Some requests failed with errors other than 429. See the lines above.")
    else:
        print("  No request was accepted. Check the key and the API address.")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--problem", type=int, default=10, help="problem id to submit to (default 10, Multiply)")
    ap.add_argument("--language", choices=list(SOLUTIONS), default="python")
    ap.add_argument("--no-poll", action="store_true", help="do not wait for verdicts after the ramp")
    args = ap.parse_args()

    env = read_env(os.path.join(ROOT, ".env"))
    key = os.environ.get("API_KEY") or env.get("API_KEY")
    base = os.environ.get("API_BASE") or env.get("API_BASE") or read_env(os.path.join(ROOT, "frontend", ".env")).get("VITE_API_BASE")
    if not key or not key.startswith("cdr_"):
        sys.exit("API_KEY (starting with cdr_) not found in .env")
    if not base:
        sys.exit("API address not found: set API_BASE in .env or VITE_API_BASE in frontend/.env")
    base = base.rstrip("/")

    # (start second, interval, how long)
    plan = [(1, 0.5, 30.0)]
    schedule, offset = [], 0.0
    for phase, interval, length in plan:
        t = 0.0
        while t < length - 1e-9:
            schedule.append((offset + t, phase))
            t += interval
        offset += length

    print("=" * 78)
    print("Submission API ramp test")
    print("=" * 78)
    print(f"API      : {base}")
    print(f"Key      : {key[:8]}…  (only the first characters are ever printed)")
    print(f"Problem  : {args.problem} ({args.language})")
    print(f"Plan     : phase 1 = 1 request / 2 s for 30 s ({sum(1 for _, p in schedule if p == 1)} requests)")
    print(f"           phase 2 = 1 request / 1 s for 30 s ({sum(1 for _, p in schedule if p == 2)} requests)")
    print("Each accepted request is a real submission judged by the engine. Ctrl-C stops early.\n")

    rec = Recorder()
    t0 = time.monotonic()
    pool = cf.ThreadPoolExecutor(max_workers=24)
    futures = []
    try:
        for i, (when, phase) in enumerate(schedule, 1):
            delay = t0 + when - time.monotonic()
            if delay > 0:
                time.sleep(delay)
            if i == 1 or schedule[i - 2][1] != phase:
                rec.say(f"\n--- phase {phase} starts at {now_label(t0)} ---")
            futures.append(pool.submit(fire, i, phase, t0, rec, base, key, args.problem, args.language))
        cf.wait(futures, timeout=60)

        accepted = [r for r in sorted(rec.rows, key=lambda r: r["n"]) if r["status"] == 200 and r["id"]]
        if accepted and not args.no_poll:
            rec.say(f"\n--- waiting for {len(accepted)} verdict(s) ---")
            with cf.ThreadPoolExecutor(max_workers=8) as ex:
                list(ex.map(lambda r: poll(r, t0, rec, base, key), accepted))
    except KeyboardInterrupt:
        print("\nInterrupted. Summarising what ran so far.")
    finally:
        pool.shutdown(wait=False, cancel_futures=True)
        summarise(rec, t0)


if __name__ == "__main__":
    main()
