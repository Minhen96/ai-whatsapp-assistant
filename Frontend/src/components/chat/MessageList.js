import React from 'react';
import { useChat } from '../../context/ChatContext';
import Message from './Message';
import LoadingMessage from './LoadingMessage';

function MessageList({ mode }) {
  const { messages, isLoading } = useChat();
  const filtered = Array.isArray(messages)
    ? messages.filter(m => !mode || m.mode === mode)
    : [];

  return (
    <div className="flex flex-col gap-4">
      {filtered.map((message) => (
        <Message key={message.id} message={message} />
      ))}
      {isLoading && <LoadingMessage />}
    </div>
  );
}

export default MessageList;
