import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Copy, Trash2, X } from "lucide-react";
import Button from "@/atoms/Button";
import Badge from "@/atoms/Badge";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import Select from "@/atoms/Select";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { DEFAULT_QUERY } from "@/lib/problemQueryUrl";
import { searchProblems } from "@/service/problemService";
import {
  testAdminService,
  type AttemptResult,
  type CreateTestInput,
  type SlotDifficulty,
  type TestDetail,
  type TestSelectionMode,
  type TestSummary,
  type TestVisibility,
} from "@/service/adminService";
import type { ApiError } from "@/service/testService";
import type { ProblemSummary } from "@/types/problemSearch";

const errorText = (e: unknown, fallback: string) => {
  const detail = (e as ApiError)?.response?.data?.detail;
  if (typeof detail === "string") return detail;
  if (Array.isArray(detail) && detail[0]?.msg) return String(detail[0].msg).replace(/^Value error, /, "");
  return fallback;
};

const linkFor = (path: string) => `${window.location.origin}${path}`;

const copy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    toast.success("Link copied");
  } catch {
    toast.error("Could not copy, select the link and copy it by hand");
  }
};

const minutes = (from: string, to: string) => Math.max(0, Math.round((new Date(to).getTime() - new Date(from).getTime()) / 60000));

/** Who took the test and what they solved. */
const Results = ({ testId }: { testId: number }) => {
  const [rows, setRows] = useState<AttemptResult[] | null>(null);
  useEffect(() => {
    setRows(null);
    testAdminService.results(testId).then(setRows).catch((e) => toast.error(errorText(e, "Failed to load results")));
  }, [testId]);

  if (rows === null) return <p className="text-sm text-muted-foreground">Loading results…</p>;
  if (rows.length === 0) return <p className="text-sm text-muted-foreground">Nobody has started this test yet.</p>;
  const ranked = [...rows].sort((a, b) => b.solvedCount - a.solvedCount || a.attemptId - b.attemptId);
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="text-[13px] text-muted-foreground">
          <tr>
            <th className="py-1 pr-3 font-medium">User</th>
            <th className="py-1 pr-3 font-medium">Solved</th>
            <th className="py-1 pr-3 font-medium">Status</th>
            <th className="py-1 font-medium">Problems</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {ranked.map((r) => (
            <tr key={r.attemptId}>
              <td className="py-1.5 pr-3 font-medium">{r.username ?? `user ${r.userId}`}</td>
              <td className="py-1.5 pr-3 font-mono">
                {r.solvedCount}/{r.total}
              </td>
              <td className="py-1.5 pr-3 text-[13px] text-muted-foreground">
                {r.over ? (r.finishedAt ? `finished after ${minutes(r.startedAt, r.finishedAt)} min` : "out of time") : "in progress"}
              </td>
              <td className="py-1.5 text-[13px]">
                {r.problems.map((p) => (
                  <span key={p.id} className={`mr-3 ${p.solved ? "text-success" : "text-muted-foreground"}`} title={`${p.submissions} submission${p.submissions === 1 ? "" : "s"}`}>
                    {p.solved ? "✓" : "✗"} {p.title}
                    {p.solvedAt ? ` (${minutes(r.startedAt, p.solvedAt)}m)` : ""}
                  </span>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

const EMPTY_FORM = {
  title: "",
  description: "",
  visibility: "PUBLIC" as TestVisibility,
  selectionMode: "ALL_PROBLEMS" as TestSelectionMode,
  problemCount: 3,
  advanced: false,
  slots: ["ANY", "ANY", "ANY"] as SlotDifficulty[],
  timed: false,
  durationMinutes: 60,
  usernames: "",
  oneAttemptOnly: true,
  expiresAt: "",
};

/** Picks problems for a test's pool by searching the problem set. */
const PoolPicker = ({
  selected,
  onChange,
}: {
  selected: Record<number, string>;
  onChange: (next: Record<number, string>) => void;
}) => {
  const [search, setSearch] = useState("");
  const debounced = useDebouncedValue(search, 250);
  const [results, setResults] = useState<ProblemSummary[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    searchProblems({ ...DEFAULT_QUERY, search: debounced, size: 12 }, controller.signal)
      .then((r) => setResults(r.problems))
      .catch(() => {});
    return () => controller.abort();
  }, [debounced]);

  const toggle = (p: ProblemSummary) => {
    const next = { ...selected };
    if (next[p.id]) delete next[p.id];
    else next[p.id] = p.title;
    onChange(next);
  };

  return (
    <div className="space-y-3">
      <Input placeholder="Search problems to add" value={search} onChange={(e) => setSearch(e.target.value)} />
      <ul className="max-h-56 divide-y divide-border overflow-auto rounded-sm border border-border">
        {results.map((p) => (
          <li key={p.id}>
            <button
              type="button"
              onClick={() => toggle(p)}
              className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-highlight-wash"
            >
              <span>
                <input type="checkbox" readOnly checked={!!selected[p.id]} className="mr-2" />
                {p.title}
              </span>
              <span className="text-[13px] text-muted-foreground">{p.difficulty}</span>
            </button>
          </li>
        ))}
        {results.length === 0 && <li className="px-3 py-2 text-sm text-muted-foreground">No problems found.</li>}
      </ul>
      <div className="flex flex-wrap gap-2">
        {Object.entries(selected).map(([id, title]) => (
          <span key={id} className="inline-flex items-center gap-1 rounded-sm border border-border px-2 py-0.5 text-[13px]">
            {title}
            <button type="button" aria-label={`Remove ${title}`} onClick={() => toggle({ id: Number(id), title } as ProblemSummary)}>
              <X size={12} />
            </button>
          </span>
        ))}
      </div>
    </div>
  );
};

/** Admin tab: build timed or untimed tests, public or sent to named users by one-off link, and manage them. */
const AdminTests = () => {
  const [form, setForm] = useState(EMPTY_FORM);
  const [pool, setPool] = useState<Record<number, string>>({});
  const [saving, setSaving] = useState(false);
  const [tests, setTests] = useState<TestSummary[]>([]);
  const [open, setOpen] = useState<TestDetail | null>(null);

  const reload = useCallback(async () => {
    try {
      setTests(await testAdminService.list());
    } catch (e) {
      toast.error(errorText(e, "Failed to load tests"));
    }
  }, []);
  useEffect(() => {
    reload();
  }, [reload]);

  const setCount = (n: number) =>
    setForm((f) => ({
      ...f,
      problemCount: n,
      slots: Array.from({ length: n }, (_, i) => f.slots[i] ?? "ANY"),
    }));
  const setSlot = (index: number, value: SlotDifficulty) =>
    setForm((f) => ({ ...f, slots: f.slots.map((s, i) => (i === index ? value : s)) }));

  const poolIds = Object.keys(pool).map(Number);
  const set = <K extends keyof typeof EMPTY_FORM>(key: K, value: (typeof EMPTY_FORM)[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  const inviteInput = (usernames: string[]): CreateTestInput["invites"] => ({
    usernames,
    expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : undefined,
  });
  const names = form.usernames.split(/[\s,]+/).filter(Boolean);

  const reportUnknown = (unknown: string[]) => {
    if (unknown.length) toast.error(`No such user: ${unknown.join(", ")}`);
  };

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const created = await testAdminService.create({
        title: form.title,
        description: form.description || undefined,
        visibility: form.visibility,
        selectionMode: form.selectionMode,
        poolProblemIds: form.selectionMode === "POOL" ? poolIds : undefined,
        problemCount: form.problemCount,
        slotDifficulties: form.advanced ? form.slots : undefined,
        durationMinutes: form.timed ? form.durationMinutes : undefined,
        multipleAttempts: !form.oneAttemptOnly,
        invites: inviteInput(form.visibility === "PRIVATE" ? names : []),
      });
      toast.success("Test created");
      reportUnknown(created.unknownUsernames);
      setForm(EMPTY_FORM);
      setPool({});
      setOpen(created.test);
      reload();
    } catch (err) {
      toast.error(errorText(err, "Failed to create test"));
    } finally {
      setSaving(false);
    }
  };

  const show = async (id: number) => {
    try {
      setOpen(await testAdminService.get(id));
    } catch (e) {
      toast.error(errorText(e, "Failed to load test"));
    }
  };

  const act = async (fn: () => Promise<unknown>, failure: string) => {
    try {
      await fn();
      if (open) setOpen(await testAdminService.get(open.id));
      reload();
    } catch (e) {
      toast.error(errorText(e, failure));
    }
  };

  const addInvites = () =>
    act(async () => {
      const res = await testAdminService.addInvites(open!.id, inviteInput(names));
      reportUnknown(res.unknownUsernames);
      set("usernames", "");
    }, "Failed to add invites");

  const remove = (t: TestSummary) => {
    if (!window.confirm(`Delete "${t.title}" and every attempt and link for it?`)) return;
    act(async () => {
      await testAdminService.remove(t.id);
      if (open?.id === t.id) setOpen(null);
    }, "Failed to delete test");
  };

  const isPrivate = form.visibility === "PRIVATE";

  return (
    <div className="grid grid-cols-1 gap-10 lg:grid-cols-2">
      <form onSubmit={create} className="space-y-5">
        <div>
          <Label htmlFor="test-title">Test title</Label>
          <Input id="test-title" value={form.title} onChange={(e) => set("title", e.target.value)} required maxLength={200} />
        </div>
        <div>
          <Label htmlFor="test-desc">Instructions (optional)</Label>
          <textarea
            id="test-desc"
            rows={3}
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm resize-none"
          />
        </div>

        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <div>
            <Label htmlFor="test-visibility">Who can take it</Label>
            <Select id="test-visibility" value={form.visibility} onChange={(e) => set("visibility", e.target.value as TestVisibility)}>
              <option value="PUBLIC">Public: anyone signed in</option>
              <option value="PRIVATE">Private: chosen users, by link</option>
            </Select>
          </div>
          <div>
            <Label htmlFor="test-mode">Problems drawn from</Label>
            <Select id="test-mode" value={form.selectionMode} onChange={(e) => set("selectionMode", e.target.value as TestSelectionMode)}>
              <option value="ALL_PROBLEMS">The whole problem set</option>
              <option value="POOL">Problems I pick</option>
            </Select>
          </div>
          <div>
            <Label htmlFor="test-count">Random problems per attempt</Label>
            <Input
              id="test-count"
              type="number"
              min={1}
              max={50}
              value={form.problemCount}
              onChange={(e) => setCount(Math.min(50, Math.max(1, parseInt(e.target.value) || 1)))}
            />
          </div>
          <div>
            <Label htmlFor="test-timed">Time limit</Label>
            <div className="flex items-center gap-3">
              <input id="test-timed" type="checkbox" checked={form.timed} onChange={(e) => set("timed", e.target.checked)} />
              <Input
                type="number"
                min={1}
                max={720}
                disabled={!form.timed}
                value={form.durationMinutes}
                onChange={(e) => set("durationMinutes", parseInt(e.target.value) || 1)}
                aria-label="Minutes"
              />
              <span className="text-sm text-muted-foreground">min</span>
            </div>
          </div>
        </div>

        <div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.advanced} onChange={(e) => set("advanced", e.target.checked)} />
            Advanced: choose the difficulty of each problem
          </label>
          {form.advanced && (
            <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-3">
              {form.slots.map((slot, i) => (
                <div key={i}>
                  <Label htmlFor={`slot-${i}`}>Problem {i + 1}</Label>
                  <Select id={`slot-${i}`} value={slot} onChange={(e) => setSlot(i, e.target.value as SlotDifficulty)}>
                    <option value="ANY">Any</option>
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HARD">Hard</option>
                  </Select>
                </div>
              ))}
            </div>
          )}
        </div>

        <div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={form.oneAttemptOnly} onChange={(e) => set("oneAttemptOnly", e.target.checked)} />
            One attempt only
          </label>
          <p className="mt-1 text-[13px] text-muted-foreground">
            {form.oneAttemptOnly
              ? "Each user gets a single attempt. For private tests the link stops working once it is finished or time runs out."
              : "Users may start again after finishing or running out of time. Each attempt draws new problems."}
          </p>
        </div>

        {form.selectionMode === "POOL" && (
          <div>
            <Label>Pool ({poolIds.length} selected)</Label>
            <PoolPicker selected={pool} onChange={setPool} />
          </div>
        )}

        {isPrivate && (
          <div className="space-y-4 border-t border-border pt-4">
            <div>
              <Label htmlFor="test-users">Usernames to invite</Label>
              <textarea
                id="test-users"
                rows={2}
                placeholder="alice, bob"
                value={form.usernames}
                onChange={(e) => set("usernames", e.target.value)}
                className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm resize-none"
              />
              <p className="mt-1 text-[13px] text-muted-foreground">Each user gets their own link, only they can open it.</p>
            </div>
            <div className="md:w-1/2">
              <Label htmlFor="test-expires">Link expires (optional)</Label>
              <Input id="test-expires" type="datetime-local" value={form.expiresAt} onChange={(e) => set("expiresAt", e.target.value)} />
            </div>
          </div>
        )}

        <Button type="submit" disabled={saving || (form.selectionMode === "POOL" && poolIds.length === 0)}>
          {saving ? "Creating…" : "Create test"}
        </Button>
      </form>

      <div className="space-y-8">
        <section>
          <h2 className="mb-3 font-serif text-2xl">Tests</h2>
          {tests.length === 0 ? (
            <p className="text-sm text-muted-foreground">No tests yet.</p>
          ) : (
            <ul className="divide-y divide-border border-y border-border">
              {tests.map((t) => (
                <li key={t.id} className="flex items-center justify-between gap-3 py-2">
                  <button type="button" onClick={() => show(t.id)} className="min-w-0 flex-1 text-left">
                    <span className="block truncate text-sm font-medium">{t.title}</span>
                    <span className="text-[13px] text-muted-foreground">
                      {t.visibility === "PUBLIC" ? "Public" : `Private, ${t.invites} link${t.invites === 1 ? "" : "s"}`} ·{" "}
                      {t.problemCount} problems · {t.durationMinutes ? `${t.durationMinutes} min` : "untimed"} ·{" "}
                      {t.multipleAttempts ? "multiple attempts" : "one attempt"} · {t.attempts} attempt
                      {t.attempts === 1 ? "" : "s"}
                    </span>
                  </button>
                  {!t.isActive && <Badge>Off</Badge>}
                  <Button variant="ghost" size="sm" aria-label={`Delete ${t.title}`} onClick={() => remove(t)}>
                    <Trash2 size={14} />
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {open && (
          <section className="space-y-4 border border-border p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="font-serif text-xl">{open.title}</h3>
                <p className="text-[13px] text-muted-foreground">
                  {open.selectionMode === "POOL" ? `${open.poolProblemIds?.length ?? 0} problems in the pool` : "Whole problem set"}
                  {open.slotDifficulties && ` · ${open.slotDifficulties.map((d) => d.toLowerCase()).join(", ")}`}
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => act(() => testAdminService.setActive(open.id, !open.isActive), "Failed to update test")}
              >
                {open.isActive ? "Turn off" : "Turn on"}
              </Button>
            </div>

            {open.visibility === "PRIVATE" ? (
              <>
                <ul className="space-y-2">
                  {open.inviteList.map((i) => (
                    <li key={i.id} className="flex items-center gap-2 text-sm">
                      <span className="w-28 truncate font-medium">{i.username ?? `user ${i.userId}`}</span>
                      <span className="flex-1 truncate font-mono text-[12px] text-muted-foreground">{linkFor(i.path)}</span>
                      <span className="text-[13px] text-muted-foreground">
                        {i.revokedAt ? "revoked" : i.used ? "used" : i.singleUse ? "one-time" : "reusable"}
                      </span>
                      <Button variant="ghost" size="sm" aria-label="Copy link" onClick={() => copy(linkFor(i.path))}>
                        <Copy size={14} />
                      </Button>
                      {!i.revokedAt && (
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label="Revoke link"
                          onClick={() => act(() => testAdminService.revokeInvite(open.id, i.id), "Failed to revoke link")}
                        >
                          <X size={14} />
                        </Button>
                      )}
                    </li>
                  ))}
                </ul>
                <div className="flex gap-2">
                  <Input
                    placeholder="More usernames, comma separated"
                    value={form.usernames}
                    onChange={(e) => set("usernames", e.target.value)}
                  />
                  <Button variant="outline" disabled={names.length === 0} onClick={addInvites}>
                    Add links
                  </Button>
                </div>
                <p className="text-[13px] text-muted-foreground">New links follow the attempt setting of this test and use the expiry from the form on the left.</p>
              </>
            ) : (
              <p className="text-sm text-muted-foreground">Public tests need no link: they are listed for every signed-in user.</p>
            )}

            <div className="border-t border-border pt-4">
              <h4 className="mb-2 font-serif text-lg">Results</h4>
              <Results testId={open.id} />
            </div>
          </section>
        )}
      </div>
    </div>
  );
};

export default AdminTests;
