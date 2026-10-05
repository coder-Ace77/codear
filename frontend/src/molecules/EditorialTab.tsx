import React, { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { editorialService, Editorial } from '@/service/editorialService';
import CodeBlock from './CodeBlock';

// Timestamps come back without a timezone and are UTC; an empty or unparsable value shows no date.
const formatDate = (value?: string) => {
    if (!value) return "";
    const d = new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(value) ? value : `${value}Z`);
    return Number.isNaN(d.getTime()) ? "" : d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
};

interface EditorialTabProps {
    problemId: number;
}

const EditorialTab: React.FC<EditorialTabProps> = ({ problemId }) => {
    const [editorials, setEditorials] = useState<Editorial[]>([]);
    const [loading, setLoading] = useState(true);
    const [isComposing, setIsComposing] = useState(false);
    const [newTitle, setNewTitle] = useState("");
    const [newContent, setNewContent] = useState("");
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        fetchEditorials();
    }, [problemId]);

    const fetchEditorials = async () => {
        setLoading(true);
        try {
            const data = await editorialService.getEditorials(problemId);
            setEditorials(data);
        } catch (err) {
            console.error("Failed to load editorials", err);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async () => {
        if (!newTitle.trim() || !newContent.trim()) return;
        setSubmitting(true);
        try {
            await editorialService.submitEditorial(problemId, newTitle, newContent);
            setIsComposing(false);
            setNewTitle("");
            setNewContent("");
            fetchEditorials();
        } catch (err) {
            console.error("Failed to submit editorial", err);
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return <p className="p-10 text-center text-muted-foreground">Loading editorials…</p>;
    }

    const field = "w-full rounded-sm border border-input bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground";

    return (
        <div className="space-y-6 px-6 py-6">
            <div className="flex items-baseline justify-between">
                <h2 className="font-serif text-2xl font-medium">Editorials</h2>
                {!isComposing && (
                    <button
                        onClick={() => setIsComposing(true)}
                        className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
                    >
                        Write one
                    </button>
                )}
            </div>

            {isComposing && (
                <div className="space-y-3 rounded-md border border-border bg-card p-4">
                    <input
                        type="text"
                        aria-label="Title"
                        placeholder="Title, for example: Approach 1, dynamic programming"
                        className={`${field} h-9`}
                        value={newTitle}
                        onChange={e => setNewTitle(e.target.value)}
                    />
                    <textarea
                        aria-label="Explanation"
                        placeholder="Write your explanation. Markdown and code fences are supported."
                        rows={8}
                        className={`${field} resize-none font-mono`}
                        value={newContent}
                        onChange={e => setNewContent(e.target.value)}
                    />
                    <div className="flex justify-end gap-2">
                        <button
                            onClick={() => setIsComposing(false)}
                            className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:bg-highlight-wash"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleSubmit}
                            disabled={submitting}
                            className="inline-flex h-7 items-center rounded-sm border border-highlight bg-highlight px-3 text-[13px] font-semibold text-highlight-foreground transition-colors hover:border-primary hover:bg-primary hover:text-primary-foreground disabled:cursor-not-allowed disabled:border-border disabled:bg-secondary disabled:text-muted-foreground"
                        >
                            {submitting ? "Submitting…" : "Submit"}
                        </button>
                    </div>
                </div>
            )}

            {editorials.length === 0 ? (
                <p className="py-12 text-center text-muted-foreground">No editorials yet. Be the first to explain this problem.</p>
            ) : (
                <div className="space-y-8">
                    {editorials.map((editorial) => (
                        <article key={editorial.id} className="border-t border-border pt-4">
                            <h3 className="font-serif text-xl font-medium leading-tight">
                                {editorial.title}
                                {editorial.isAdmin && (
                                    <span className="mark-fill ml-2 px-1.5 py-0.5 align-middle font-sans text-[11px] font-semibold uppercase tracking-[0.06em]">
                                        Official
                                    </span>
                                )}
                            </h3>
                            <p className="mb-3 mt-1 font-mono text-[13px] text-muted-foreground">
                                {editorial.username}{formatDate(editorial.createdAt) && ` · ${formatDate(editorial.createdAt)}`}
                            </p>

                            <div className="prose prose-sm max-w-none font-serif text-[17px] leading-[28px] text-foreground prose-headings:font-serif prose-headings:font-medium prose-headings:text-foreground prose-a:text-foreground prose-a:underline prose-strong:text-foreground">
                                <ReactMarkdown
                                    components={{
                                        code({ inline, className, children, ...props }: any) {
                                            const match = /language-(\w+)/.exec(className || '');
                                            return !inline && match ? (
                                                <CodeBlock
                                                    code={String(children).replace(/\n$/, '')}
                                                    language={match[1]}
                                                    className="my-3"
                                                />
                                            ) : (
                                                <code className="rounded-sm bg-secondary px-1.5 py-0.5 font-mono text-sm text-foreground" {...props}>
                                                    {children}
                                                </code>
                                            );
                                        }
                                    }}
                                >
                                    {editorial.content}
                                </ReactMarkdown>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </div>
    );
};

export default EditorialTab;
