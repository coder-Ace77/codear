"""Shared pieces for the scripts that talk to the Codear API with an API key. Standard library only."""
import json
import os
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read_env(path):
    """KEY=value lines of a .env file, without quotes. A missing file is empty."""
    values = {}
    try:
        with open(path) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, value = line.split("=", 1)
                    values[key.strip()] = value.strip().strip("'\"")
    except FileNotFoundError:
        pass
    return values


def load_settings():
    """(api base address, api key) from the repo's .env files."""
    key = read_env(os.path.join(ROOT, ".env")).get("API_KEY")
    base = read_env(os.path.join(ROOT, "frontend", ".env")).get("VITE_API_BASE")
    if not key or not key.startswith("cdr_"):
        raise SystemExit("API_KEY (starting with cdr_) not found in .env")
    if not base:
        raise SystemExit("VITE_API_BASE not found in frontend/.env")
    return base.rstrip("/"), key


class ApiClient:
    def __init__(self, base, key):
        self.base = base
        self.key = key

    def call(self, method, path, body=None, timeout=40):
        """(status, parsed json). Network errors come back as status 0."""
        request = urllib.request.Request(
            self.base + path, data=json.dumps(body).encode() if body is not None else None, method=method)
        request.add_header("X-API-Key", self.key)
        request.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.status, json.loads(response.read().decode() or "null")
        except urllib.error.HTTPError as e:
            raw = e.read().decode()
            try:
                return e.code, json.loads(raw)
            except ValueError:
                return e.code, {"detail": raw[:200]}
        except Exception as e:  # timeout, connection refused
            return 0, {"detail": str(e)}

    def submit(self, problem_id, language, code):
        """The submission id, or raises RuntimeError with the server's reason."""
        status, body = self.call("POST", "/problem/submit",
                                 {"problemId": problem_id, "language": language, "code": code})
        if status != 200:
            raise RuntimeError(f"submit failed: {status} {body}")
        return body["submissionId"]

    def watch(self, submission_id, on_update=None, give_up_after=240):
        """Follows a submission with the progress long-poll and returns the final answer."""
        since, started = 0, time.monotonic()
        while time.monotonic() - started < give_up_after:
            status, body = self.call("GET", f"/problem/submissions/{submission_id}/progress?since={since}&wait=20")
            if status == 404 and "detail" in body and body["detail"] == "Not Found":
                raise RuntimeError("this API has no /progress endpoint yet: deploy the new problem service first")
            if status != 200:
                time.sleep(1)
                continue
            if not body["changed"]:
                continue
            since = body["version"]
            if on_update:
                on_update(body)
            if body["terminal"]:
                return body
        raise RuntimeError(f"no verdict within {give_up_after} seconds")
