import { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { CHAT_MODES } from '../utils/constants';
import { createMessage, createErrorMessage } from '../utils/messageHelpers';
import { sendChatMessage } from '../services/api/chatApi';
import { sendWhatsAppMessage } from '../services/api/whatsappApi';

export const useChatMessages = (mode) => {
  const { addMessage, setLoading, userId } = useChat();

  const handleSendMessage = async (message) => {
    if (!message.trim()) return;

    // Add user message
    const userMessage = createMessage(message, 'user', mode);
    addMessage(userMessage);

    setLoading(true);

    try {
      let data;

      // Route to appropriate service based on mode
      if (mode === CHAT_MODES.WHATSAPP) {
        data = await sendWhatsAppMessage(message);
      } else {
        data = await sendChatMessage(message, mode);
      }

      // Add bot response with documents if available
      const botMessage = createMessage(
        data.response || data.message || "Sorry, I couldn't process your request.",
        'bot',
        mode
      );

      // Attach documents if they exist in the response
      if (data.documents && Array.isArray(data.documents)) {
        botMessage.documents = data.documents;
        botMessage.hasDocuments = data.documents.length > 0;
      }

      // Store userId for downloads
      if (!botMessage.userId) {
        botMessage.userId = userId || data.userId;
      }

      addMessage(botMessage);

    } catch (error) {
      console.error('Error sending message:', error);
      addMessage(createErrorMessage(error, mode));
    } finally {
      setLoading(false);
    }
  };

  return { handleSendMessage };
};