import React, { useState, useRef } from 'react';
import { Send, Loader2 } from 'lucide-react';

function MessageInput({ onSendMessage, placeholder = "Type your message..." }) {
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const textareaRef = useRef(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!message.trim() || isLoading) return;

    setIsLoading(true);
    try {
      await onSendMessage(message);
      setMessage('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
        textareaRef.current.focus(); // ✅ keeps focus after sending
      }
    } catch (error) {
      console.error('Error sending message:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {   // ✅ changed from handleKeyPress
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleInputChange = (e) => {
    setMessage(e.target.value);

    // Auto-resize textarea
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 120) + 'px';
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="flex items-end gap-2 rounded-2xl border border-gray-200 bg-white/80 px-3 py-2 focus-within:ring-2 focus-within:ring-brand">
      <textarea
        ref={textareaRef}
        value={message}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        className="flex-1 resize-none bg-transparent outline-none text-[15px] leading-relaxed placeholder:text-gray-500 max-h-32"
        rows="1"
        disabled={isLoading}
        autoFocus   // ✅ will focus when mounted
      />
        <button
          type="submit"
          className={`inline-flex items-center justify-center w-10 h-10 rounded-full bg-brand text-white ${(!message.trim() || isLoading) ? 'opacity-60 cursor-not-allowed' : 'hover:brightness-110'}`}
          disabled={!message.trim() || isLoading}
        >
          {isLoading ? (
            <Loader2 size={18} className="animate-spin" />
          ) : (
            <Send size={18} />
          )}
        </button>
      </div>
    </form>
  );
}

export default MessageInput;
