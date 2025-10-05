import React from 'react';

function MessageBubble({ message }) {
  const isUser = message.type === 'user';
  const time = new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const mode = message.mode;

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[75%] rounded-2xl px-4 py-2 shadow-sm ${
        isUser
          ? 'bg-gradient-to-tr from-brand to-brand-dark text-white rounded-br-md'
          : 'bg-white border border-gray-200 text-gray-900 rounded-bl-md'
      }`}>
        <div className="whitespace-pre-wrap leading-relaxed text-[15px]">{message.content}</div>
        {mode && (
          <div className={`mt-1 text-[10px] ${isUser ? 'text-white/70' : 'text-gray-500'}`}>
            {mode === 'chat' ? 'Chat' : mode === 'store' ? 'Store' : mode === 'whatsapp' ? 'WhatsApp' : mode}
          </div>
        )}
        <div className={`mt-1 text-[11px] flex items-center gap-1 ${isUser ? 'text-white/80' : 'text-gray-500'}`}>
          <span>{time}</span>
        </div>
      </div>
    </div>
  );
}

export default MessageBubble;


