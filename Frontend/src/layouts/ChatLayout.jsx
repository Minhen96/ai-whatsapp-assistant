import React, { useEffect } from 'react';
import { useChat } from '../context/ChatContext';
import { useWebSocketSync } from '../hooks/useWebSocketSync';
import { useWelcomeMessage } from '../hooks/useWelcomeMessage';
import ChatHeader from '../components/chat/ChatHeader';
import ChatMessages from '../components/chat/ChatMessages';
import ChatFooter from '../components/chat/ChatFooter';

const ChatLayout = ({ 
  mode, 
  onBack, 
  onConnectionChange,
  onSendMessage,
  onFileUpload,
  isUploading
}) => {
  const { addMessage, setConnection } = useChat();
  const { isConnected: wsConnected } = useWebSocketSync();

  // Initialize welcome message for this mode
  useWelcomeMessage(mode, addMessage);

  // Handle connection status
  useEffect(() => {
    setConnection(wsConnected);
    onConnectionChange?.(wsConnected);

    return () => {
      setConnection(false);
      onConnectionChange?.(false);
    };
  }, [wsConnected, setConnection, onConnectionChange]);

  return (
    <div className="flex flex-col h-[calc(90vh-64px)] bg-white/60">
      <ChatHeader 
        mode={mode} 
        onBack={onBack} 
        isConnected={wsConnected} 
      />
      
      <ChatMessages mode={mode} />
      
      <ChatFooter 
        mode={mode}
        onSendMessage={onSendMessage}
        onFileUpload={onFileUpload}
        isUploading={isUploading}
      />
    </div>
  );
};

export default ChatLayout;