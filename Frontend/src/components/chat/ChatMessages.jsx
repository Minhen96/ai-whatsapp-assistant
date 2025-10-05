import React from 'react';
import MessageList from './MessageList';
import { useAutoScroll } from '../../hooks/useAutoScroll';
import { useChat } from '../../context/ChatContext';

const ChatMessages = ({ mode }) => {
  const { messages } = useChat();
  const scrollRef = useAutoScroll([messages]);

  return (
    <div className="flex-1 overflow-y-auto p-4 md:p-6 bg-gray-50/50">
      <div className="max-w-2xl mx-auto">
        <MessageList mode={mode} />
        <div ref={scrollRef} />
      </div>
    </div>
  );
};

export default ChatMessages;