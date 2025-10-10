import { useEffect } from 'react';
import { useWebSocket } from './useWebSocket';
import { useChat } from '../context/ChatContext';
import { getWsUrl } from '../utils/apiHelpers';

export const useWebSocketSync = () => {
  const { addMessage, setConnection } = useChat();
  
  const { isConnected, lastMessage, error } = useWebSocket(
    getWsUrl(`/ws/chat?userId=frontend-user`),
    {
      onMessage: (data) => {
        console.log('Received WebSocket message:', data);
        
        if (data.type === 'whatsapp_message') {
          // Add WhatsApp message to chat
          const whatsappMessage = {
            id: Date.now(),
            type: 'bot',
            content: `📱 WhatsApp: ${data.message}`,
            timestamp: new Date(data.timestamp),
            source: 'whatsapp'
          };
          addMessage(whatsappMessage);
          
          // Add AI response
          const aiResponse = {
            id: Date.now() + 1,
            type: 'bot',
            content: data.response,
            timestamp: new Date(data.timestamp + 1000),
            source: 'whatsapp'
          };
          addMessage(aiResponse);
        } else if (data.type === 'frontend_message') {
          // Sync with other frontend clients
          if (data.userId !== 'frontend-user') {
            const syncMessage = {
              id: Date.now(),
              type: 'bot',
              content: `👤 Other user: ${data.message}`,
              timestamp: new Date(data.timestamp),
              source: 'sync'
            };
            addMessage(syncMessage);
          }
        } else if (data.type === 'system_message') {
          // System notifications
          const systemMessage = {
            id: Date.now(),
            type: 'bot',
            content: `🔔 System: ${data.message}`,
            timestamp: new Date(data.timestamp),
            source: 'system'
          };
          addMessage(systemMessage);
        }
      },
      onOpen: () => {
        console.log('WebSocket connected for sync');
        setConnection(true);
      },
      onClose: () => {
        console.log('WebSocket disconnected');
        setConnection(false);
      },
      onError: (error) => {
        console.error('WebSocket sync error:', error);
        setConnection(false);
      }
    }
  );

  useEffect(() => {
    setConnection(isConnected);
  }, [isConnected, setConnection]);

  return {
    isConnected,
    error
  };
};
