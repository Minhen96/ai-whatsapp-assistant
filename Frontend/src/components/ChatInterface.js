import React, { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Send, Upload, Loader2 } from 'lucide-react';
import { useChat } from '../context/ChatContext';
import { useWebSocketSync } from '../hooks/useWebSocketSync';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import FileUploader from './FileUploader';

function ChatInterface({ mode, onBack, onConnectionChange }) {
  const { addMessage, setLoading, setConnection } = useChat();
  const [isUploading, setIsUploading] = useState(false);
  const messagesEndRef = useRef(null);
  
  // Initialize WebSocket sync
  const { isConnected: wsConnected, error: wsError } = useWebSocketSync();

    useEffect(() => {
        // Auto-scroll whenever messages change
        if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [addMessage]); // depends on new messages

  // Only push a welcome message once per mode
  useEffect(() => {
    setConnection(wsConnected);
    onConnectionChange?.(wsConnected);
  
    const hasWelcomedKey = `welcomed-${mode}`;
    if (sessionStorage.getItem(hasWelcomedKey) !== '1') {
      addMessage({
        id: Date.now(),
        type: 'bot',
        content: getWelcomeMessage(mode),
        timestamp: new Date()
      });
      sessionStorage.setItem(hasWelcomedKey, '1');
    }
  
    return () => {
      setConnection(false);
      onConnectionChange?.(false);
    };
  }, [mode, wsConnected, addMessage, setConnection, onConnectionChange]);

  const getWelcomeMessage = (currentMode) => {
    switch (currentMode) {
      case 'chat':
        return "🤖 Hello! I'm your AI assistant. Ask me anything and I'll help you with intelligent responses.";
      case 'store':
        return "📚 Knowledge storage mode activated! Send me text or upload documents to store in your knowledge base.";
      case 'whatsapp':
        return "📱 WhatsApp sync mode! Your messages here will be synced with WhatsApp. Start chatting!";
      default:
        return "Hello! How can I help you today?";
    }
  };

  const handleSendMessage = async (message) => {
    if (!message.trim()) return;
  
    // Add user message
    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: message,
      timestamp: new Date(),
      mode
    };
    addMessage(userMessage);
  
    setLoading(true);
  
    try {
      // Determine endpoint and request configuration based on mode
      let endpoint, requestInit;
      
      if (mode === 'whatsapp') {
        // Use relative URL to leverage proxy, or absolute if proxy fails
        endpoint = process.env.NODE_ENV === 'development' 
          ? '/whatsapp/incoming_manual'
          : 'http://localhost:8080/whatsapp/incoming_manual';
        requestInit = {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ 
            from: 'frontend-user', 
            body: message 
          }).toString()
        };
      } else {
        endpoint = process.env.NODE_ENV === 'development'
          ? '/chat'
          : 'http://localhost:8080/chat';
        requestInit = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            message, 
            mode, 
            userId: 'frontend-user' 
          })
        };
      }
  
      // Make the API call
      const response = await fetch(endpoint, requestInit);
  
      if (!response.ok) {
        const errorText = await response.text();
        console.error('Backend error:', errorText);
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }
      
      const data = await response.json();
  
      // Add bot response
      const botMessage = {
        id: Date.now() + 1,
        type: 'bot',
        content: data.response || data.message || 'Sorry, I couldn\'t process your request.',
        timestamp: new Date(),
        mode
      };
      addMessage(botMessage);
  
    } catch (error) {
      console.error('Error sending message:', error);
      const errorMessage = {
        id: Date.now() + 1,
        type: 'bot',
        content: `Sorry, there was an error: ${error.message}. Please try again.`,
        timestamp: new Date(),
        mode
      };
      addMessage(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (file) => {
    setIsUploading(true);
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', 'frontend-user');

    try {
      const response = await fetch('/knowledge/store', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('Upload error:', errorText);
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }
      
      const data = await response.json();
      
      const botMessage = {
        id: Date.now(),
        type: 'bot',
        content: data.message || 'File uploaded successfully!',
        timestamp: new Date(),
        mode: 'store'
      };
      addMessage(botMessage);

    } catch (error) {
      console.error('Error uploading file:', error);
      const errorMessage = {
        id: Date.now(),
        type: 'bot',
        content: `Sorry, there was an error uploading your file: ${error.message}`,
        timestamp: new Date(),
        mode: 'store'
      };
      addMessage(errorMessage);
    } finally {
      setIsUploading(false);
    }
  };

  const getModeTitle = () => {
    switch (mode) {
      case 'chat': return 'Chat with AI';
      case 'store': return 'Store Knowledge';
      case 'whatsapp': return 'WhatsApp Sync';
      default: return 'AI Assistant';
    }
  };

  const getModeIcon = () => {
    switch (mode) {
      case 'chat': return '🤖';
      case 'store': return '📚';
      case 'whatsapp': return '📱';
      default: return '🤖';
    }
  };

  return (
    <div className="flex flex-col h-[calc(90vh-64px)] bg-white/60">
      <div className="flex items-center gap-3 px-6 py-3 border-b border-white/40 bg-white/60">
        <button 
          className="inline-flex items-center justify-center w-9 h-9 rounded-lg bg-gray-100 hover:bg-gray-200 transition" 
          onClick={onBack}
          aria-label="Go back"
        >
          <ArrowLeft size={18} />
        </button>
        <div className="flex items-center gap-2">
          <span className="text-lg">{getModeIcon()}</span>
          <h3 className="text-base font-medium tracking-tight">{getModeTitle()}</h3>
        </div>
        <div className="ml-auto flex items-center gap-2 text-sm text-gray-600">
          <span 
            className={`inline-block w-2 h-2 rounded-full ${
              wsConnected ? 'bg-green-500' : 'bg-gray-300'
            } animate-pulse`}
            title={wsConnected ? 'Connected to server' : 'Disconnected from server'}
          ></span>
          {wsConnected ? 'Connected' : 'Disconnected'}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 md:p-6 bg-gray-50/50">
        <div className="max-w-2xl mx-auto">
          <MessageList mode={mode} />
          <div ref={messagesEndRef} />
        </div>
      </div>

      <div className="px-4 md:px-6 py-4 bg-white/70 border-t border-white/40">
        <div className="max-w-2xl mx-auto space-y-3">
          {mode === 'store' && (
            <FileUploader onFileUpload={handleFileUpload} isUploading={isUploading} />
          )}
          <MessageInput 
            onSendMessage={handleSendMessage} 
            placeholder={getInputPlaceholder(mode)} 
          />
        </div>
      </div>
    </div>
  );
}

const getInputPlaceholder = (mode) => {
  switch (mode) {
    case 'chat': return 'Ask me anything...';
    case 'store': return 'Type text or upload a file...';
    case 'whatsapp': return 'Type a message to sync with WhatsApp...';
    default: return 'Type your message...';
  }
};

export default ChatInterface;