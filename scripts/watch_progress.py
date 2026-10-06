#!/usr/bin/env python3
"""Submit a solution and watch it being judged, update by update.

    python3 scripts/watch_progress.py <problem-id> <cpp|python> <source file>

Uses API_KEY from .env and the API address from frontend/.env. Prints every progress update the
long-poll delivers, with the time since submitting, then a summary of how smooth the updates were.
Standard library only.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def env(path):
    out = {}
    for line in open(path):
        if "=" in line and not line.startswith("#"):
            k, v = line.strip().split("=", 1)
            out[k] = v.strip("'\"")
    return out


def call(method, url, key, body=None, timeout=40):
    req = urllib.request.Request(url, data=json.dumps(body).encode() if body else None, method=method)
    req.add_header("X-API-Key", key)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, {"detail": e.read().decode()[:200]}


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    pid, lang, path = int(sys.argv[1]), sys.argv[2], sys.argv[3]
    key = env(os.path.join(ROOT, ".env"))["API_KEY"]
    api = env(os.path.join(ROOT, "frontend", ".env"))["VITE_API_BASE"].rstrip("/")

    t0 = time.monotonic()
    status, body = call("POST", f"{api}/problem/submit", key, {"problemId": pid, "language": lang, "code": open(path).read()})
    if status != 200:
        sys.exit(f"submit failed: {status} {body}")
    sid = body["submissionId"]
    print(f"submitted {sid[:8]} in {time.monotonic() - t0:.2f}s\n")
    print(f"{'time':>7}  {'ver':>3}  {'stage':<10} {'%':>4}  message")

    since, updates, requests_made, last_change = 0, [], 0, time.monotonic()
    final = None
    while time.monotonic() - t0 < 240:
        status, p = call("GET", f"{api}/problem/submissions/{sid}/progress?since={since}&wait=20", key)
        requests_made += 1
        if status == 404 and "Not Found" in str(p):
            sys.exit("This API has no /progress endpoint yet: deploy the new problem service first.")
        if status != 200:
            print(f"        progress -> {status} {p}")
            time.sleep(1)
            continue
        if not p["changed"]:
            continue
        now = time.monotonic()
        updates.append((now - t0, now - last_change))
        last_change = now
        since = p["version"]
        print(f"{now - t0:6.2f}s  {p['version']:>3}  {p['stage']:<10} {p['percent']:>3}%  {p['message']}")
        if p["terminal"]:
            final = p
            break

    print()
    if not final:
        sys.exit("no verdict within 4 minutes")
    r = final.get("result") or {}
    print(f"verdict {final['status']}  tests {r.get('passedTests')}/{r.get('totalTests')}  judge cpu {r.get('timeTakenMs')} ms  {r.get('memoryUsed')}")
    gaps = [g for _, g in updates[1:]]
    print(f"{len(updates)} updates in {updates[-1][0]:.1f}s using {requests_made} requests"
          + (f"; longest wait between updates {max(gaps):.2f}s" if gaps else ""))


if __name__ == "__main__":
    main()
