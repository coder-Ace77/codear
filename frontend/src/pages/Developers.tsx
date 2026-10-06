import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import toast from "react-hot-toast";
import { format } from "date-fns";
import CodeBlock, { CopyButton } from "@/molecules/CodeBlock";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import Button from "@/atoms/Button";
import { parseUtc as utc } from "@/lib/time";
import { apiKeyService, ApiKey, CreatedApiKey } from "@/service/apiKeyService";

const BASE = (import.meta.env.VITE_API_BASE as string) || "https://your-api-host/api/v1";

const Heading = ({ children }: { children: React.ReactNode }) => (
  <h2 className="overline mb-3 mt-12 border-b border-foreground pb-2">{children}</h2>
);

const Prose = ({ children }: { children: React.ReactNode }) => (
  <p className="mb-3 max-w-[70ch] font-serif text-lg leading-[30px]">{children}</p>
);

const Code = ({ children }: { children: React.ReactNode }) => (
  <code className="rounded-sm bg-secondary px-1.5 py-0.5 font-mono text-sm">{children}</code>
);

const submitExample = `curl -X POST "${BASE}/problem/submit" \\
  -H "X-API-Key: $CODEAR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"problemId": 10, "language": "python", "code": "a, b = map(int, input().split())\\nprint(a * b)"}'

# {"message": "Code submitted successfully", "submissionId": "0b6f..."}`;

const resultExample = `curl "${BASE}/problem/submissions/0b6f..." \\
  -H "X-API-Key: $CODEAR_API_KEY"

# {"submissionId": "0b6f...", "status": "PASSED", "passedTests": 12, "totalTests": 12,
#  "timeTakenMs": 38, "memoryUsed": "17.2 MB", "language": "python", ...}`;

const progressExample = `curl "${BASE}/problem/submissions/0b6f.../progress?since=0&wait=20" \\
  -H "X-API-Key: $CODEAR_API_KEY"

# {"version": 7, "stage": "RUNNING", "message": "Running test 7 of 26", "percent": 53,
#  "total": 26, "started": 7, "completed": 6, "terminal": false, "changed": true,
#  "status": "IN_PROGRESS", "submissionId": "0b6f..."}

# when "terminal" is true the same response carries the verdict under "result"`;

const pythonExample = `import os
import time

import requests

API = "${BASE}"
HEADERS = {"X-API-Key": os.environ["CODEAR_API_KEY"]}

code = "a, b = map(int, input().split())\\nprint(a * b)"

sent = requests.post(
    f"{API}/problem/submit",
    headers=HEADERS,
    json={"problemId": 10, "language": "python", "code": code},
)
if sent.status_code == 429:  # one submission per 10 seconds
    time.sleep(int(sent.headers.get("Retry-After", 10)))
    raise SystemExit("Rate limited: try again")
sent.raise_for_status()
submission_id = sent.json()["submissionId"]

# Each request waits up to 10 seconds for a verdict, so a few polls is enough.
while True:
    result = requests.get(f"{API}/problem/submissions/{submission_id}", headers=HEADERS)
    result.raise_for_status()
    body = result.json()
    if body["status"] != "IN_PROGRESS":
        break

print(body["status"], f'{body["passedTests"]}/{body["totalTests"]} tests', body["timeTakenMs"], "ms")`;

const KeysPanel = () => {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [name, setName] = useState("");
  const [created, setCreated] = useState<CreatedApiKey | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setKeys(await apiKeyService.list());
      setSignedIn(true);
    } catch (e: any) {
      if (e.response?.status === 401) setSignedIn(false);
      else toast.error("Could not load your API keys.");
    }
  }, []);

  useEffect(() => {
    // Only the token matters here; /user/user would also work but keys are the point of this page.
    if (!localStorage.getItem("token")) {
      setSignedIn(false);
      return;
    }
    load();
  }, [load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      const key = await apiKeyService.create(name.trim());
      setCreated(key);
      setName("");
      await load();
    } catch (err: any) {
      const detail = err.response?.data?.detail;
      toast.error(typeof detail === "string" ? detail : "Could not create the key.");
    } finally {
      setBusy(false);
    }
  };

  const revoke = async (key: ApiKey) => {
    if (!confirm(`Revoke "${key.name}"? Anything using it will stop working immediately.`)) return;
    try {
      await apiKeyService.revoke(key.id);
      if (created?.id === key.id) setCreated(null);
      toast.success("Key revoked");
      await load();
    } catch {
      toast.error("Could not revoke the key.");
    }
  };

  if (signedIn === null) return <p className="text-muted-foreground">Loading…</p>;

  if (!signedIn) {
    return (
      <Prose>
        <Link to="/login" className="font-medium underline underline-offset-[3px]">
          Sign in
        </Link>{" "}
        to create an API key.
      </Prose>
    );
  }

  return (
    <div>
      {created && (
        <div className="mb-6 rounded-md border border-border bg-highlight-wash p-4" role="status">
          <p className="mb-2 font-medium">Copy your new key now. It will not be shown again.</p>
          <div className="flex items-center gap-2">
            <code className="min-w-0 flex-1 overflow-x-auto whitespace-nowrap rounded-sm bg-card px-3 py-2 font-mono text-sm">
              {created.key}
            </code>
            <CopyButton text={created.key} />
          </div>
          <button
            onClick={() => setCreated(null)}
            className="mt-3 text-[13px] font-medium underline underline-offset-[3px]"
          >
            I have saved it
          </button>
        </div>
      )}

      <form onSubmit={create} className="mb-6 flex max-w-xl items-end gap-3">
        <div className="flex-1">
          <Label htmlFor="key-name">Key name</Label>
          <Input
            id="key-name"
            placeholder="for example: my laptop script"
            value={name}
            maxLength={60}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <Button type="submit" variant="accent" disabled={busy || !name.trim()}>
          Create key
        </Button>
      </form>

      {keys.length === 0 ? (
        <p className="text-muted-foreground">You have no active keys.</p>
      ) : (
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-foreground">
              {["Name", "Key", "Created", "Last used", ""].map((h) => (
                <th key={h} className="overline px-3 py-2 text-left">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {keys.map((k) => (
              <tr key={k.id} className="border-b border-border">
                <td className="px-3 py-3 font-medium">{k.name}</td>
                <td className="px-3 py-3 font-mono text-[13px] text-muted-foreground">{k.prefix}…</td>
                <td className="whitespace-nowrap px-3 py-3 text-[13px] text-muted-foreground">
                  {format(utc(k.createdAt), "MMM d, yyyy")}
                </td>
                <td className="whitespace-nowrap px-3 py-3 text-[13px] text-muted-foreground">
                  {k.lastUsedAt ? format(utc(k.lastUsedAt), "MMM d, yyyy HH:mm") : "Never"}
                </td>
                <td className="px-3 py-3 text-right">
                  <button
                    onClick={() => revoke(k)}
                    className="text-[13px] font-medium text-danger underline underline-offset-[3px]"
                  >
                    Revoke
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <p className="mt-3 text-[13px] text-muted-foreground">You can have up to 5 active keys.</p>
    </div>
  );
};

const Developers = () => {
  return (
    <main className="mx-auto max-w-[1100px] px-6 pb-20 pt-12">
      <h1 className="font-serif text-5xl font-medium leading-[1.05] tracking-[-0.02em] md:text-[56px] md:leading-[60px]">
        API access
      </h1>
      <p className="mt-2 max-w-[60ch] text-muted-foreground">
        Submit solutions and read verdicts from your own scripts, editor plugins or CI, using a key instead of
        your password.
      </p>

      <Heading>Your API keys</Heading>
      <KeysPanel />

      <Heading>Quick start</Heading>
      <Prose>
        Send your key in the <Code>X-API-Key</Code> header on every request. <Code>Authorization: Bearer cdr_…</Code>{" "}
        works too. Keys act as you: they can submit code and read your own results, and nothing else. They cannot
        create keys or perform admin actions.
      </Prose>

      <h3 className="mb-2 mt-6 font-serif text-2xl font-medium">1. Submit a solution</h3>
      <Prose>
        <Code>language</Code> is <Code>python</Code> or <Code>cpp</Code>. Find a problem's id in the address of its
        page, for example <Code>/coding/10</Code>. Code can be up to 64 KB.
      </Prose>
      <CodeBlock code={submitExample} language="bash" />

      <h3 className="mb-2 mt-8 font-serif text-2xl font-medium">2. Get the result</h3>
      <Prose>
        Each request holds the connection for up to 10 seconds, waiting for the judge. If{" "}
        <Code>status</Code> is still <Code>IN_PROGRESS</Code> when it returns, ask again.
      </Prose>
      <CodeBlock code={resultExample} language="bash" />

      <h3 className="mb-2 mt-8 font-serif text-2xl font-medium">Watching progress (optional)</h3>
      <Prose>
        To follow a submission while it is judged, long-poll <Code>/progress</Code>. Start with{" "}
        <Code>since=0</Code>, then repeat with <Code>since=</Code> the <Code>version</Code> of the last answer. Each
        call returns the moment something newer exists, or after <Code>wait</Code> seconds (up to 25) with{" "}
        <Code>changed: false</Code>. <Code>stage</Code> moves through <Code>QUEUED</Code>,{" "}
        <Code>PREPARING</Code>, <Code>RUNNING</Code> (with the test number), <Code>JUDGING</Code> and{" "}
        <Code>DONE</Code>. Stop when <Code>terminal</Code> is true: that response also holds the full verdict.
      </Prose>
      <CodeBlock code={progressExample} language="bash" />

      <table className="mt-6 w-full max-w-xl border-collapse">
        <thead>
          <tr className="border-b border-foreground">
            <th className="overline px-3 py-2 text-left">status</th>
            <th className="overline px-3 py-2 text-left">Meaning</th>
          </tr>
        </thead>
        <tbody>
          {[
            ["IN_PROGRESS", "Queued or being judged. Poll again."],
            ["PASSED", "Every test passed."],
            ["FAILED", "Judged, and at least one test failed or the code did not run."],
          ].map(([s, m]) => (
            <tr key={s} className="border-b border-border">
              <td className="px-3 py-2 font-mono text-[13px]">{s}</td>
              <td className="px-3 py-2">{m}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 className="mb-2 mt-8 font-serif text-2xl font-medium">A complete example</h3>
      <CodeBlock code={pythonExample} language="python" />

      <Heading>Limits</Heading>
      <Prose>
        You can submit <strong>one solution every 10 seconds</strong>. The limit belongs to you, not to a key, so
        the website and all your keys share it. When you go over, the API answers{" "}
        <Code>429 Too Many Requests</Code> with a <Code>Retry-After</Code> header giving the seconds to wait.
      </Prose>

      <table className="w-full max-w-xl border-collapse">
        <thead>
          <tr className="border-b border-foreground">
            <th className="overline px-3 py-2 text-left">Code</th>
            <th className="overline px-3 py-2 text-left">Meaning</th>
          </tr>
        </thead>
        <tbody>
          {[
            ["401", "Missing, invalid or revoked key."],
            ["404", "No such submission, or it belongs to someone else."],
            ["422", "The request body is invalid, for example code over 64 KB."],
            ["429", "Too many submissions. Wait for Retry-After seconds."],
          ].map(([c, m]) => (
            <tr key={c} className="border-b border-border">
              <td className="px-3 py-2 font-mono text-[13px]">{c}</td>
              <td className="px-3 py-2">{m}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <Heading>Keep your key safe</Heading>
      <Prose>
        Treat a key like a password: keep it in an environment variable, not in code you share. We store only a
        fingerprint of it, so we cannot show it again or recover it. If one leaks, revoke it above and make a new
        one.
      </Prose>
    </main>
  );
};

export default Developers;
