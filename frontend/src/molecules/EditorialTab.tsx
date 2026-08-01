import React, { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { editorialService, Editorial } from '@/service/editorialService';
import { Loader2, Plus, PenTool, Check, ShieldCheck, X } from 'lucide-react';
import CodeBlock from './CodeBlock';

const mono = "font-['Space_Mono']";
const serif = "font-['Cormorant_Garamond']";

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
        return (
            <div className="flex justify-center items-center gap-3 p-10">
                <Loader2 className="animate-spin text-brand-orange" />
                <span className={`${mono} text-xs tracking-[0.2em] uppercase text-muted-foreground`}>Loading editorials</span>
            </div>
        );
    }

    return (
        <div className="p-4 md:p-6 space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <p className={`${mono} text-[10px] tracking-[0.3em] uppercase text-brand-orange mb-1`}>Community</p>
                    <h2 className={`${serif} text-3xl text-foreground`}>Editorials</h2>
                </div>
                {!isComposing && (
                    <button
                        onClick={() => setIsComposing(true)}
                        className={`${mono} flex items-center gap-2 text-[11px] tracking-[0.15em] uppercase px-4 py-2.5 rounded-full border border-border text-foreground hover:border-brand-orange hover:text-brand-orange transition-colors`}
                    >
                        <Plus size={15} /> Write
                    </button>
                )}
            </div>

            {isComposing && (
                <div className="bg-card border border-border rounded-lg p-4 space-y-4">
                    <input
                        type="text"
                        placeholder="Title — e.g. Approach 1: Dynamic Programming"
                        className="w-full bg-background border border-border rounded-md px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-brand-orange transition-colors"
                        value={newTitle}
                        onChange={e => setNewTitle(e.target.value)}
                    />
                    <textarea
                        placeholder="Write your explanation here... Markdown & code fences supported!"
                        rows={8}
                        className="w-full bg-background border border-border rounded-md px-3 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-brand-orange transition-colors resize-none font-mono"
                        value={newContent}
                        onChange={e => setNewContent(e.target.value)}
                    />
                    <div className="flex justify-end gap-2">
                        <button
                            onClick={() => setIsComposing(false)}
                            className={`${mono} flex items-center gap-1.5 text-[11px] tracking-[0.15em] uppercase px-3 py-2 text-muted-foreground hover:text-foreground transition-colors`}
                        >
                            <X size={14} /> Cancel
                        </button>
                        <button
                            onClick={handleSubmit}
                            disabled={submitting}
                            className={`${mono} flex items-center gap-1.5 text-[11px] tracking-[0.15em] uppercase px-4 py-2 rounded-full bg-brand-olive text-background hover:opacity-90 transition-opacity disabled:opacity-50`}
                        >
                            {submitting ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                            Submit
                        </button>
                    </div>
                </div>
            )}

            {editorials.length === 0 ? (
                <div className="text-center text-muted-foreground py-12">
                    <PenTool size={30} className="mx-auto mb-3 text-brand-orange opacity-70" />
                    <p className={`${mono} text-xs tracking-[0.15em] uppercase`}>No editorials yet — be the first to explain this problem</p>
                </div>
            ) : (
                <div className="space-y-4">
                    {editorials.map((editorial) => (
                        <div
                            key={editorial.id}
                            className={`rounded-lg border p-4 ${editorial.isAdmin ? 'bg-brand-yellow/[0.06] border-brand-yellow/30' : 'bg-card border-border'}`}
                        >
                            <div className="flex items-center gap-3 mb-4">
                                <div className="w-9 h-9 rounded-full bg-brand-orange/20 flex items-center justify-center text-sm font-bold text-brand-orange">
                                    {editorial.username.charAt(0).toUpperCase()}
                                </div>
                                <div>
                                    <h3 className={`${serif} font-semibold text-foreground text-lg leading-tight flex items-center gap-2`}>
                                        {editorial.title}
                                        {editorial.isAdmin && (
                                            <span className={`${mono} flex items-center gap-1 text-[9px] bg-brand-yellow/10 text-brand-yellow px-1.5 py-0.5 rounded border border-brand-yellow/30 uppercase tracking-[0.15em] font-bold`}>
                                                <ShieldCheck size={10} /> Official
                                            </span>
                                        )}
                                    </h3>
                                    <p className={`${mono} text-[10px] tracking-[0.1em] text-muted-foreground mt-0.5`}>
                                        {editorial.username} · {new Date(editorial.createdAt).toLocaleDateString()}
                                    </p>
                                </div>
                            </div>

                            <div className="prose prose-invert prose-sm max-w-none text-foreground/80 prose-headings:text-foreground prose-a:text-brand-orange prose-strong:text-foreground">
                                <ReactMarkdown
                                    components={{
                                        code({ inline, className, children, ...props }: any) {
                                            const match = /language-(\w+)/.exec(className || '');
                                            return !inline && match ? (
                                                <CodeBlock
                                                    code={String(children).replace(/\n$/, '')}
                                                    language={match[1]}
                                                    dots={false}
                                                    className="my-3"
                                                />
                                            ) : (
                                                <code className="bg-secondary px-1.5 py-0.5 rounded text-brand-orange font-mono text-xs" {...props}>
                                                    {children}
                                                </code>
                                            );
                                        }
                                    }}
                                >
                                    {editorial.content}
                                </ReactMarkdown>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default EditorialTab;
