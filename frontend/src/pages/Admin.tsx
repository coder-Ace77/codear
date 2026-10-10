import { useState, useEffect } from "react";
import { Navigate } from "react-router-dom";
import type { User } from "@/types/User";
import Button from "@/atoms/Button";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import Select from "@/atoms/Select";
import Badge from "@/atoms/Badge";
import { CHECKER_OPTIONS, DEFAULT_CHECKER, DEFAULT_FLOAT_TOLERANCE } from "@/constants/checkerModes";
import AdminLive from "@/molecules/AdminLive";
import AdminTests from "@/molecules/AdminTests";
import { Plus, X, Trash2 } from "lucide-react";
import apiClient from "@/lib/apiClient";
import { Textarea } from "@/components/ui/textarea";
import toast from "react-hot-toast";
import TagsSelector from "@/components/ui/Problemtags";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { searchProblems, deleteProblem } from "@/service/problemService";
import { DEFAULT_QUERY } from "@/lib/problemQueryUrl";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const Admin = () => {
  const [activeTab, setActiveTab] = useState("create");

  // undefined = still checking, null = not signed in. The API enforces the role on every
  // request; this only keeps non-admins from seeing the page.
  const [me, setMe] = useState<User | null | undefined>(undefined);
  useEffect(() => {
    apiClient
      .get("/user/user")
      .then((r) => setMe(r.data as User))
      .catch(() => setMe(null));
  }, []);

  // Create Problem State
  const [formData, setFormData] = useState({
    title: "",
    difficulty: "Easy",
    description: "",
    constraints: "",
    exampleInput: "",
    exampleOutput: "",
    timeLimitMs: 1000,
    memoryLimitMb: 256,
    checker: DEFAULT_CHECKER,
    checkerTolerance: DEFAULT_FLOAT_TOLERANCE,
  });

  const [tags, setTags] = useState<string[]>([]);
  const [testCases, setTestCases] = useState([{ input: "", output: "" }]);

  // Manage Problems State
  const [problems, setProblems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  // --- Create Problem Handlers ---

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const sampleTestCase = {
      input: formData.exampleInput,
      output: formData.exampleOutput,
      isSample: true,
    };

    const otherTestCases = testCases.map(tc => ({
      input: tc.input,
      output: tc.output,
      isSample: false,
    }));

    const payload = {
      ...formData,
      tags: tags,
      testCases: [sampleTestCase, ...otherTestCases],
      timeLimitMs: formData.timeLimitMs,
      memoryLimitMb: formData.memoryLimitMb,
      checker: formData.checker,
      // only the number-tolerance checker uses it
      checkerTolerance: formData.checker === "FLOAT" ? formData.checkerTolerance : undefined,
    };
    try {
      const response = await apiClient.post("/problem/addproblem", payload);
      console.log("Problem added successfully:", response.data);
      toast.success("Problem added successfully");

      setFormData({
        title: "",
        difficulty: "Easy",
        description: "",
        constraints: "",
        exampleInput: "",
        exampleOutput: "",
        timeLimitMs: 1000,
        memoryLimitMb: 256,
        checker: DEFAULT_CHECKER,
        checkerTolerance: DEFAULT_FLOAT_TOLERANCE,
      });
      setTestCases([{ input: "", output: "" }]);
      setTags([]);

    } catch (error) {
      console.error("Failed to add problem:", error);
      toast.error("Failed to add problem");
    }
  };

  const addTestCase = () => {
    setTestCases([...testCases, { input: "", output: "" }]);
  };

  const removeTestCase = (index: number) => {
    setTestCases(testCases.filter((_, i) => i !== index));
  };

  const updateTestCase = (index: number, field: "input" | "output", value: string) => {
    const newTestCases = [...testCases];
    newTestCases[index][field] = value;
    setTestCases(newTestCases);
  };

  // --- Manage Problems Handlers ---

  const loadProblems = () => {
    setLoading(true);
    searchProblems({ ...DEFAULT_QUERY, sort: "newest", size: 50 })
      .then((found) => setProblems(found.problems))
      .catch(() => toast.error("Failed to load problems"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (activeTab === "manage") {
      loadProblems();
    }
  }, [activeTab]);

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this problem?")) return;
    try {
      await deleteProblem(id);
      toast.success("Problem deleted successfully");
      loadProblems(); // Refresh list
    } catch (error) {
      toast.error("Failed to delete problem");
    }
  };


  if (me === undefined) {
    return <p className="p-12 text-center text-muted-foreground">Checking access…</p>;
  }
  if (!me || me.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return (
    <main className="mx-auto max-w-[1600px] px-6 pb-16 pt-12">
      <div>
        <div className="mb-6">
          <h1 className="font-serif text-5xl font-medium leading-[1.05] tracking-[-0.02em] md:text-[56px] md:leading-[60px]">
            Admin
          </h1>
          <p className="mt-1 text-muted-foreground">Create and manage coding problems.</p>
        </div>

        <Tabs defaultValue="create" value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="mb-6 flex h-auto w-full justify-start gap-6 rounded-none border-b border-border bg-transparent p-0">
            <TabsTrigger
              value="create"
              className="rounded-none border-0 bg-transparent px-0 pb-2 pt-0 text-sm font-medium text-muted-foreground shadow-none data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-[inset_0_-2px_0_hsl(var(--ink))]"
            >
              Create problem
            </TabsTrigger>
            <TabsTrigger
              value="manage"
              className="rounded-none border-0 bg-transparent px-0 pb-2 pt-0 text-sm font-medium text-muted-foreground shadow-none data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-[inset_0_-2px_0_hsl(var(--ink))]"
            >
              Manage problems
            </TabsTrigger>
            <TabsTrigger
              value="tests"
              className="rounded-none border-0 bg-transparent px-0 pb-2 pt-0 text-sm font-medium text-muted-foreground shadow-none data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-[inset_0_-2px_0_hsl(var(--ink))]"
            >
              Tests
            </TabsTrigger>
            <TabsTrigger
              value="live"
              className="rounded-none border-0 bg-transparent px-0 pb-2 pt-0 text-sm font-medium text-muted-foreground shadow-none data-[state=active]:bg-transparent data-[state=active]:text-foreground data-[state=active]:shadow-[inset_0_-2px_0_hsl(var(--ink))]"
            >
              Live activity
            </TabsTrigger>
          </TabsList>

          <TabsContent value="create">
            <div>
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Basic Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="md:col-span-2">
                    <Label htmlFor="title">Problem title</Label>
                    <Input
                      id="title"
                      placeholder="Two Sum"
                      value={formData.title}
                      onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                      required
                    />
                  </div>

                  <div>
                    <Label htmlFor="difficulty">Difficulty</Label>
                    <Select
                      id="difficulty"
                      value={formData.difficulty}
                      onChange={(e) => setFormData({ ...formData, difficulty: e.target.value })}
                    >
                      <option value="Easy">Easy</option>
                      <option value="Medium">Medium</option>
                      <option value="Hard">Hard</option>
                    </Select>
                  </div>

                  <TagsSelector tags={tags} setTags={setTags}></TagsSelector>
                </div>

                <div>
                  <Label htmlFor="description">Problem description</Label>
                  <textarea
                    id="description"
                    rows={6}
                    placeholder="Describe the problem in detail"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground resize-none"
                    required
                  />
                </div>

                {/* Example */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <Label htmlFor="exampleInput">Example input</Label>
                    <textarea
                      id="exampleInput"
                      rows={4}
                      placeholder="1 100 200"
                      value={formData.exampleInput}
                      onChange={(e) => setFormData({ ...formData, exampleInput: e.target.value })}
                      className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground resize-none font-mono text-sm"
                      required
                    />
                  </div>

                  <div>
                    <Label htmlFor="exampleOutput">Example output</Label>
                    <textarea
                      id="exampleOutput"
                      rows={4}
                      placeholder="2"
                      value={formData.exampleOutput}
                      onChange={(e) => setFormData({ ...formData, exampleOutput: e.target.value })}
                      className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground resize-none font-mono text-sm"
                      required
                    />
                  </div>
                </div>


                {/* Time and Memory Limits */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <Label htmlFor="timeLimit">Time limit (ms)</Label>
                    <Input
                      id="timeLimit"
                      type="number"
                      placeholder="1000"
                      value={formData.timeLimitMs}
                      onChange={(e) => setFormData({ ...formData, timeLimitMs: parseInt(e.target.value) || 0 })}
                      required
                    />
                  </div>
                  <div>
                    <Label htmlFor="memoryLimit">Memory limit (MB)</Label>
                    <Input
                      id="memoryLimit"
                      type="number"
                      placeholder="256"
                      value={formData.memoryLimitMb}
                      onChange={(e) => setFormData({ ...formData, memoryLimitMb: parseInt(e.target.value) || 0 })}
                      required
                    />
                  </div>
                </div>

                {/* How answers are compared */}
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                  <div>
                    <Label htmlFor="checker">Answer checking</Label>
                    <Select
                      id="checker"
                      value={formData.checker}
                      onChange={(e) => setFormData({ ...formData, checker: e.target.value })}
                    >
                      {CHECKER_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </Select>
                    <p className="mt-1 text-[13px] text-muted-foreground">
                      {CHECKER_OPTIONS.find((o) => o.value === formData.checker)?.hint}
                    </p>
                  </div>
                  {formData.checker === "FLOAT" && (
                    <div>
                      <Label htmlFor="checkerTolerance">Allowed difference</Label>
                      <Input
                        id="checkerTolerance"
                        type="number"
                        step="any"
                        min="0"
                        value={formData.checkerTolerance}
                        onChange={(e) => setFormData({ ...formData, checkerTolerance: parseFloat(e.target.value) || 0 })}
                        required
                      />
                    </div>
                  )}
                </div>

                {/* Test Cases */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <Label>Test cases</Label>
                    <Button type="button" variant="secondary" size="sm" onClick={addTestCase}>
                      <Plus className="h-4 w-4" aria-hidden="true" />
                      Add test case
                    </Button>
                  </div>

                  <div className="space-y-4">
                    {testCases.map((testCase, index) => (
                      <div key={index} className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 rounded-md bg-secondary border border-border">
                        <div>
                          <Label htmlFor={`test-input-${index}`}>Input {index + 1}</Label>
                          <Textarea
                            id={`test-input-${index}`}
                            placeholder="5 abc"
                            value={testCase.input}
                            onChange={(e) => updateTestCase(index, "input", e.target.value)}
                            className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground resize-none font-mono text-sm"
                            required
                          />
                        </div>
                        <div>
                          <div className="flex items-center justify-between mb-1.5">
                            <Label htmlFor={`test-output-${index}`}>Expected output {index + 1}</Label>
                            {testCases.length > 1 && (
                              <button
                                type="button"
                                onClick={() => removeTestCase(index)}
                                aria-label="Remove test case"
                                className="text-danger transition-colors hover:opacity-70"
                              >
                                <X className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                          <Textarea
                            id={`test-output-${index}`}
                            placeholder="0"
                            value={testCase.output}
                            onChange={(e) => updateTestCase(index, "output", e.target.value)}
                            className="w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground resize-none font-mono text-sm"
                            required
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="flex justify-end pt-2">
                  <Button type="submit" variant="accent" size="lg">
                    Create problem
                  </Button>
                </div>
              </form>
            </div>
          </TabsContent>

          <TabsContent value="manage">
            <div>
              <div className="mb-4 flex items-baseline justify-between">
                <h2 className="font-serif text-2xl font-medium">Existing problems</h2>
                <Button variant="outline" size="sm" onClick={loadProblems}>Refresh</Button>
              </div>

              {loading ? (
                <p className="py-8 text-center text-muted-foreground">Loading problems…</p>
              ) : (
                <div>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[100px]">ID</TableHead>
                        <TableHead>Title</TableHead>
                        <TableHead>Difficulty</TableHead>
                        <TableHead>Tags</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {problems.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={5} className="text-center h-24 text-muted-foreground">
                            No problems found.
                          </TableCell>
                        </TableRow>
                      ) : (
                        problems.map((problem) => (
                          <TableRow key={problem.id}>
                            <TableCell className="font-medium">{problem.id}</TableCell>
                            <TableCell>{problem.title}</TableCell>
                            <TableCell>
                              <Badge variant={problem.difficulty.toLowerCase() as "easy" | "medium" | "hard"}>
                                {problem.difficulty}
                              </Badge>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap gap-1">
                                {problem.tags && problem.tags.map(tag => (
                                  <span key={tag} className="inline-flex h-6 items-center rounded-full border border-border px-2 text-xs text-muted-foreground">{tag}</span>
                                ))}
                              </div>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => handleDelete(problem.id)}
                              >
                                <Trash2 className="h-4 w-4" aria-hidden="true" />
                                Delete
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          </TabsContent>
          <TabsContent value="tests">
            {activeTab === "tests" && <AdminTests />}
          </TabsContent>

          <TabsContent value="live">
            {activeTab === "live" && <AdminLive />}
          </TabsContent>
        </Tabs>
      </div>
    </main>
  );
};

export default Admin;
