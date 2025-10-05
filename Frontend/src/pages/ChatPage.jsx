import React from 'react';
import ChatLayout from '../layouts/ChatLayout';
import { useChatMessages } from '../hooks/useChatMessages';
import { useFileUpload } from '../hooks/useFileUpload';

const ChatPage = ({ mode, onBack, onConnectionChange }) => {
  const { handleSendMessage } = useChatMessages(mode);
  const { handleFileUpload, isUploading } = useFileUpload();

  return (
    <ChatLayout
      mode={mode}
      onBack={onBack}
      onConnectionChange={onConnectionChange}
      onSendMessage={handleSendMessage}
      onFileUpload={handleFileUpload}
      isUploading={isUploading}
    />
  );
};

export default ChatPage;