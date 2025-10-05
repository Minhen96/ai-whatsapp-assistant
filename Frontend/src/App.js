import React, { useState } from 'react';
import ChatPage from './pages/ChatPage';
import ModeSelector from './components/ui/ModeSelector';
import { ChatProvider } from './context/ChatContext';
import './index.css';

function App() {
  const [currentMode, setCurrentMode] = useState(null);
  const [isConnected, setIsConnected] = useState(false);

  return (
    <ChatProvider>
      <div className="min-h-full flex items-center justify-center p-4 md:p-8">
        <div className="w-full max-w-4xl h-[90vh] glass rounded-2xl overflow-hidden">
          <header className="px-6 py-4 border-b border-white/40 bg-white/60 backdrop-blur-xl">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-semibold tracking-tight">AI Assistant</h1>
                <p className="text-sm text-gray-600">Chat or store knowledge.</p>
              </div>
              {isConnected && (
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <span className="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
                  Connected
                </div>
              )}
            </div>
          </header>
          {!currentMode ? (
            <ModeSelector onModeSelect={setCurrentMode} />
          ) : (
            <ChatPage
              mode={currentMode}
              onBack={() => setCurrentMode(null)}
              onConnectionChange={setIsConnected}
            />
          )}
        </div>
      </div>
    </ChatProvider>
  );
}

export default App;