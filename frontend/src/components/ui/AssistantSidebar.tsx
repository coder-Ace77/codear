import React, { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { atomDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { aiService } from '@/service/aiService';
import { MessageSquare, ChevronLeft, ChevronRight, Send, Loader2, Copy, Check, Bot, User } from 'lucide-react';

interface AssistantSidebarProps {
  problemStatement: string;
  code: string;
  problemId: string;
}

const AssistantSidebar = ({ problemStatement, code, problemId }: AssistantSidebarProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [chatHistory, setChatHistory] = useState<{ role: 'user' | 'assistant', content: string }[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatHistory, isLoading]);

  useEffect(() => {
    const fetchHistory = async () => {
      // Lazy load: only fetch if open and problemId exists
      if (!problemId || !isOpen) return;

      try {
        const history = await aiService.getChatHistory(problemId);
        if (Array.isArray(history)) {
          setChatHistory(history.map((msg: any) => ({
            role: msg.role === 'assistant' ? 'assistant' : 'user',
            content: msg.content
          })));
        }
      } catch (err) {
        console.error("Failed to fetch chat history", err);
      }
    };
    fetchHistory();
    // Depend on isOpen to trigger fetch when sidebar opens
  }, [problemId, isOpen]);

  const handleSendMessage = async () => {
    if (!message.trim() || isLoading) return;

    const userMsg = message;
    setChatHistory(prev => [...prev, { role: 'user', content: userMsg }]);
    setMessage("");
    setIsLoading(true);

    try {
      const response = await aiService.chatWithAssistant(problemStatement, code, userMsg, problemId);
      setChatHistory(prev => [...prev, { role: 'assistant', content: response.reply || response.message }]);
    } catch (err) {
      setChatHistory(prev => [...prev, { role: 'assistant', content: "⚠️ Failed to connect to AI. Please check your API or rate limit." }]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Sidebar Container */}
      <div
        className={`fixed left-0 top-16 h-[calc(100vh-4rem)] z-50 flex transform transition-transform duration-300 ease-in-out ${isOpen ? 'translate-x-0' : '-translate-x-full'
          }`}
      >
        <div className="flex flex-col w-[85vw] sm:w-[450px] h-full bg-[#0d1117] border-r border-slate-700 shadow-2xl">

          <div className="p-4 border-b border-slate-800 bg-[#161b22] flex items-center gap-2">
            <Bot className="text-blue-400" size={20} />
            <span className="font-semibold text-slate-200 tracking-tight">AI Coding Buddy</span>
          </div>

          <div className="flex-grow overflow-y-auto p-4 space-y-6 custom-scrollbar bg-[#0d1117]">
            {chatHistory.length === 0 && (
              <div className="flex flex-col items-center justify-center text-slate-500 mt-20 text-sm gap-4">
                <Bot size={48} className="text-slate-700" />
                <p>Ask me about the problem statement or your current code.</p>
              </div>
            )}

            {chatHistory.map((chat, i) => (
              <div key={i} className={`flex gap-3 ${chat.role === 'user' ? 'flex-row-reverse' : ''}`}>
                <div className={`w-8 h-8 rounded-sm flex items-center justify-center shrink-0 ${chat.role === 'user' ? 'bg-indigo-600' : 'bg-slate-700'}`}>
                  {chat.role === 'user' ? <User size={16} /> : <Bot size={16} />}
                </div>
                <div className={`max-w-[85%] rounded-sm px-4 py-3 text-sm leading-relaxed shadow-sm ${chat.role === 'user' ? 'bg-indigo-700 text-white' : 'bg-[#161b22] text-slate-200 border border-slate-700'}`}>
                  <ReactMarkdown
                    components={{
                      code({ inline, className, children, ...props }: any) {
                        const match = /language-(\w+)/.exec(className || '');
                        const codeString = String(children).replace(/\n$/, '');

                        const [isCopied, setIsCopied] = useState(false);

                        const handleCopy = () => {
                          navigator.clipboard.writeText(codeString);
                          setIsCopied(true);
                          setTimeout(() => setIsCopied(false), 2000);
                        };

                        return !inline && match ? (
                          <div className="relative group my-4 rounded-md border border-slate-700 overflow-hidden bg-[#0d1117]">
                            <div className="flex items-center justify-between px-3 py-2 bg-[#161b22] border-b border-slate-700 text-xs text-slate-400 font-mono">
                              <span>{match[1]}</span>
                              <button
                                onClick={handleCopy}
                                className="flex items-center gap-1.5 hover:text-white transition-colors"
                                title="Copy code"
                              >
                                {isCopied ? <Check size={14} className="text-green-400" /> : <Copy size={14} />}
                                <span>{isCopied ? 'Copied!' : 'Copy'}</span>
                              </button>
                            </div>
                            <SyntaxHighlighter
                              style={atomDark}
                              language={match[1]}
                              PreTag="div"
                              className="!bg-[#0d1117] !p-4 !m-0 !text-[13px] overflow-x-auto"
                              {...props}
                            >
                              {codeString}
                            </SyntaxHighlighter>
                          </div>
                        ) : (
                          <code className="bg-[#1f2428] px-1.5 py-0.5 rounded text-pink-300 font-mono text-xs border border-slate-700" {...props}>
                            {children}
                          </code>
                        );
                      }
                    }}
                  >
                    {chat.content}
                  </ReactMarkdown>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex gap-3 animate-pulse">
                <div className="w-8 h-8 rounded-sm bg-slate-700 shrink-0" />
                <div className="h-10 w-2/3 bg-[#161b22] rounded-sm border border-slate-700" />
              </div>
            )}
            <div ref={chatEndRef} />
          </div>

          <div className="p-4 bg-[#161b22] border-t border-slate-800">
            <div className="relative flex items-center">
              <textarea
                rows={1}
                className="w-full bg-[#0d1117] text-slate-200 text-sm p-3 pr-12 rounded-sm outline-none border border-slate-700 focus:border-blue-500 transition-colors resize-none placeholder-slate-600"
                placeholder="Ask a question..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
              />
              <button
                onClick={handleSendMessage}
                disabled={isLoading || !message.trim()}
                className="absolute right-2 p-2 text-blue-500 hover:text-blue-400 disabled:text-slate-600 transition-colors"
              >
                <Send size={18} />
              </button>
            </div>
          </div>
        </div>

        {/* Toggle Button moved outside the clipping area if needed, or attached to the sliding div */}
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={`absolute -right-12 top-20 h-12 w-12 flex items-center justify-center rounded-r-xl transition-all shadow-lg 
            ${isOpen
              ? 'bg-[#161b22] text-slate-400 border border-l-0 border-slate-700'
              : 'bg-primary text-primary-foreground shadow-glow animate-pulse hover:animate-none'
            }`}
          style={{ pointerEvents: 'auto' }}
        >
          {isOpen ? <ChevronLeft size={24} /> : <Bot size={24} />}
        </button>
      </div>

      {/* Overlay for mobile when open */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 sm:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
};

export default AssistantSidebar;