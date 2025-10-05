import { useEffect } from 'react';
import { MODE_CONFIG } from '../utils/constants';
import { createMessage } from '../utils/messageHelpers';

export const useWelcomeMessage = (mode, addMessage) => {
  useEffect(() => {
    const hasWelcomedKey = `welcomed-${mode}`;
    
    if (sessionStorage.getItem(hasWelcomedKey) !== '1') {
      const welcomeMessage = createMessage(
        MODE_CONFIG[mode]?.welcomeMessage || 'Hello! How can I help you today?',
        'bot'
      );
      addMessage(welcomeMessage);
      sessionStorage.setItem(hasWelcomedKey, '1');
    }
  }, [mode, addMessage]);
};