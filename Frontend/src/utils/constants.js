export const CHAT_MODES = {
    CHAT: 'chat',
    STORE: 'store',
    WHATSAPP: 'whatsapp'
  };
  
  export const MODE_CONFIG = {
    [CHAT_MODES.CHAT]: {
      title: 'Chat with AI',
      icon: '🤖',
      placeholder: 'Ask me anything...',
      welcomeMessage: "🤖 Hello! I'm your AI assistant. Ask me anything and I'll help you with intelligent responses."
    },
    [CHAT_MODES.STORE]: {
      title: 'Store Knowledge',
      icon: '📚',
      placeholder: 'Type text or upload a file...',
      welcomeMessage: "📚 Knowledge storage mode activated! Send me text or upload documents to store in your knowledge base."
    },
    [CHAT_MODES.WHATSAPP]: {
      title: 'WhatsApp Sync',
      icon: '📱',
      placeholder: 'Type a message to sync with WhatsApp...',
      welcomeMessage: "📱 WhatsApp sync mode! Your messages here will be synced with WhatsApp. Start chatting!"
    }
  };
  
  export const API_ENDPOINTS = {
    CHAT: '/chat',
    WHATSAPP: '/whatsapp/incoming_manual',
    KNOWLEDGE_STORE: '/knowledge/store'
  };