import { useState, useEffect } from "react";
import Button from "@/atoms/Button";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import Select from "@/atoms/Select";
import { Plus, X, Trash2 } from "lucide-react";
import apiClient from "@/lib/apiClient";
import { Textarea } from "@/components/ui/textarea";
import toast from "react-hot-toast";
import TagsSelector from "@/components/ui/Problemtags";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { fetchProblems, deleteProblem } from "@/service/problemService";
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
      memoryLimitMb: formData.memoryLimitMb
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
    fetchProblems({
      page: 1,
      search: "",
      difficulty: "all",
      sortBy: "newest",
      tag: "",
      onSuccess: (data) => {
        setProblems(data.problems);
        setLoading(false);
      },
      onError: (err) => {
        toast.error("Failed to load problems");
        setLoading(false);
      }
    });
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


  return (
    <div className="min-h-screen py-10 md:py-14 px-6 md:px-10">
      <div className="max-w-6xl mx-auto">
        <div className="mb-10 border-b border-border pb-8">
          <p className="font-['Space_Mono'] text-[11px] tracking-[0.35em] text-muted-foreground mb-4">
            CONTROL&nbsp;ROOM
          </p>
          <h1 className="font-['Cormorant_Garamond'] uppercase leading-[0.9] text-5xl md:text-7xl text-foreground">
            Admin
          </h1>
          <p className="font-['Space_Mono'] text-xs tracking-[0.15em] text-muted-foreground mt-4">
            Manage coding problems
          </p>
        </div>

        <Tabs defaultValue="create" value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="inline-flex h-auto gap-2 bg-transparent p-0 mb-8">
            <TabsTrigger
              value="create"
              className="font-['Space_Mono'] text-[11px] tracking-[0.2em] uppercase rounded-full border border-border px-6 py-2.5 data-[state=active]:bg-accent data-[state=active]:text-accent-foreground data-[state=active]:border-accent data-[state=active]:shadow-none"
            >
              Create Problem
            </TabsTrigger>
            <TabsTrigger
              value="manage"
              className="font-['Space_Mono'] text-[11px] tracking-[0.2em] uppercase rounded-full border border-border px-6 py-2.5 data-[state=active]:bg-accent data-[state=active]:text-accent-foreground data-[state=active]:border-accent data-[state=active]:shadow-none"
            >
              Manage Problems
            </TabsTrigger>
          </TabsList>

          <TabsContent value="create">
            <div className="bg-card border border-border rounded-lg p-6 md:p-8">
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Basic Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="md:col-span-2">
                    <Label htmlFor="title">Problem Title</Label>
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
                  <Label htmlFor="description">Problem Description</Label>
                  <textarea
                    id="description"
                    rows={6}
                    placeholder="Describe the problem in detail..."
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none"
                    required
                  />
                </div>

                {/* Example */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <Label htmlFor="exampleInput">Example Input</Label>
                    <textarea
                      id="exampleInput"
                      rows={4}
                      placeholder="1 100 200"
                      value={formData.exampleInput}
                      onChange={(e) => setFormData({ ...formData, exampleInput: e.target.value })}
                      className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none font-mono text-sm"
                      required
                    />
                  </div>

                  <div>
                    <Label htmlFor="exampleOutput">Example Output</Label>
                    <textarea
                      id="exampleOutput"
                      rows={4}
                      placeholder="2"
                      value={formData.exampleOutput}
                      onChange={(e) => setFormData({ ...formData, exampleOutput: e.target.value })}
                      className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none font-mono text-sm"
                      required
                    />
                  </div>
                </div>


                {/* Time and Memory Limits */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <Label htmlFor="timeLimit">Time Limit (ms)</Label>
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
                    <Label htmlFor="memoryLimit">Memory Limit (MB)</Label>
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

                {/* Test Cases */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <Label>Test Cases</Label>
                    <Button type="button" variant="secondary" size="sm" onClick={addTestCase}>
                      <Plus className="w-4 h-4 mr-1" />
                      Add Test Case
                    </Button>
                  </div>

                  <div className="space-y-4">
                    {testCases.map((testCase, index) => (
                      <div key={index} className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 rounded-lg bg-secondary border border-border">
                        <div>
                          <Label htmlFor={`test-input-${index}`}>Input {index + 1}</Label>
                          <Textarea
                            id={`test-input-${index}`}
                            placeholder="5 abc"
                            value={testCase.input}
                            onChange={(e) => updateTestCase(index, "input", e.target.value)}
                            className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none font-mono text-sm"
                            required
                          />
                        </div>
                        <div>
                          <div className="flex items-center justify-between mb-1.5">
                            <Label htmlFor={`test-output-${index}`}>Expected Output {index + 1}</Label>
                            {testCases.length > 1 && (
                              <button
                                type="button"
                                onClick={() => removeTestCase(index)}
                                className="text-destructive hover:text-destructive/80 transition-colors"
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
                            className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none font-mono text-sm"
                            required
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="flex gap-4 pt-4">
                  <Button type="submit" variant="primary" size="lg" className="flex-1">
                    Create Problem
                  </Button>
                </div>
              </form>
            </div>
          </TabsContent>

          <TabsContent value="manage">
            <div className="bg-card border border-border rounded-lg p-6 md:p-8">
              <div className="flex justify-between items-center mb-6">
                <h2 className="font-['Cormorant_Garamond'] text-3xl md:text-4xl text-foreground">Existing Problems</h2>
                <Button variant="outline" size="sm" onClick={loadProblems}>Refresh</Button>
              </div>

              {loading ? (
                <div>Loading problems...</div>
              ) : (
                <div className="border rounded-md">
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
                              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-medium border ${
                                problem.difficulty === "Easy"
                                  ? "bg-brand-olive/20 text-brand-olive border-brand-olive/40"
                                  : problem.difficulty === "Medium"
                                  ? "bg-brand-yellow/20 text-brand-yellow border-brand-yellow/40"
                                  : "bg-brand-orange/20 text-brand-orange border-brand-orange/40"
                              }`}>
                                {problem.difficulty}
                              </span>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap gap-1">
                                {problem.tags && problem.tags.map(tag => (
                                  <span key={tag} className="text-xs bg-secondary px-2 py-1 rounded">{tag}</span>
                                ))}
                              </div>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => handleDelete(problem.id)}
                              >
                                <Trash2 className="w-4 h-4 mr-1" />
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
        </Tabs>
      </div >
    </div >
  );
};

export default Admin;
