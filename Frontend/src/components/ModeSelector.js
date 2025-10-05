import React from 'react';
import { MessageCircle, Database, Smartphone } from 'lucide-react';

function ModeSelector({ onModeSelect }) {
  const modes = [
    {
      id: 'chat',
      title: 'Chat with AI',
      description: 'Ask questions and get AI-powered responses',
      icon: MessageCircle,
      tone: 'from-brand to-brand-dark text-white',
    },
    {
      id: 'store',
      title: 'Store Knowledge',
      description: 'Upload documents and text to build your knowledge base',
      icon: Database,
      tone: 'from-emerald-400 to-emerald-500 text-white',
    },
    {
      id: 'whatsapp',
      title: 'WhatsApp Sync',
      description: 'Connect to WhatsApp for seamless messaging',
      icon: Smartphone,
      tone: 'from-rose-400 to-rose-500 text-white',
    },
  ];

  return (
    <div className="p-6 md:p-10 h-full flex flex-col justify-center">
      <div className="text-center mb-8">
        <h2 className="text-2xl font-semibold tracking-tight">Choose Your Mode</h2>
        <p className="text-gray-600 mt-1">Select how you want to interact with the AI Assistant</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 max-w-4xl mx-auto w-full">
        {modes.map((mode) => {
          const Icon = mode.icon;
          return (
            <button
              key={mode.id}
              onClick={() => onModeSelect(mode.id)}
              className="group relative overflow-hidden rounded-2xl border border-gray-200 bg-white/70 p-5 text-left shadow-soft transition hover:shadow-lg hover:translate-y-[-2px]"
            >
              <div className={`mb-4 inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-tr ${mode.tone}`}>
                <Icon size={24} />
              </div>
              <h3 className="text-lg font-medium tracking-tight">{mode.title}</h3>
              <p className="mt-1 text-sm text-gray-600">{mode.description}</p>
              <div className="absolute right-4 top-4 text-gray-400 transition group-hover:translate-x-1">→</div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default ModeSelector;
