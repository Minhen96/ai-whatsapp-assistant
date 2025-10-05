import React, { createContext, useContext, useReducer, useEffect } from 'react';

const ChatContext = createContext();

const initialState = {
  messages: [],
  isLoading: false,
  currentMode: null,
  userId: null,
  isConnected: false
};

function chatReducer(state, action) {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    case 'ADD_MESSAGE':
      return { 
        ...state, 
        messages: [...state.messages, action.payload] 
      };
    case 'SET_MESSAGES':
      return { ...state, messages: action.payload };
    case 'SET_MODE':
      return { ...state, currentMode: action.payload };
    case 'SET_USER_ID':
      return { ...state, userId: action.payload };
    case 'SET_CONNECTION':
      return { ...state, isConnected: action.payload };
    case 'CLEAR_MESSAGES':
      return { ...state, messages: [] };
    default:
      return state;
  }
}

export function ChatProvider({ children }) {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  // Hydrate from localStorage once
  useEffect(() => {
    try {
      const saved = localStorage.getItem('chat_messages');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          dispatch({ type: 'SET_MESSAGES', payload: parsed });
        }
      }
    } catch {}
  }, []);

  // Persist messages on change
  useEffect(() => {
    try {
      localStorage.setItem('chat_messages', JSON.stringify(state.messages));
    } catch {}
  }, [state.messages]);

  const addMessage = (message) => {
    const messageWithDefaults = {
      id: message.id ?? Date.now(),
      timestamp: message.timestamp ?? new Date(),
      ...message,
    };
    dispatch({ type: 'ADD_MESSAGE', payload: messageWithDefaults });
  };

  const setLoading = (loading) => {
    dispatch({ type: 'SET_LOADING', payload: loading });
  };

  const setMode = (mode) => {
    dispatch({ type: 'SET_MODE', payload: mode });
  };

  const setUserId = (userId) => {
    dispatch({ type: 'SET_USER_ID', payload: userId });
  };

  const setConnection = (connected) => {
    dispatch({ type: 'SET_CONNECTION', payload: connected });
  };

  const clearMessages = () => {
    dispatch({ type: 'CLEAR_MESSAGES' });
  };

  const value = {
    ...state,
    addMessage,
    setLoading,
    setMode,
    setUserId,
    setConnection,
    clearMessages
  };

  return (
    <ChatContext.Provider value={value}>
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
}
