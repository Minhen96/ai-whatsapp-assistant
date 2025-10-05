import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { MODE_CONFIG } from '../../utils/constants';
import ConnectionStatus from '../ui/ConnectionStatus';
import ModeIcon from '../ui/ModeIcon';

const ChatHeader = ({ mode, onBack, isConnected }) => {
  const config = MODE_CONFIG[mode];

  return (
    <div className="flex items-center gap-3 px-6 py-3 border-b border-white/40 bg-white/60">
      <button 
        className="inline-flex items-center justify-center w-9 h-9 rounded-lg bg-gray-100 hover:bg-gray-200 transition" 
        onClick={onBack}
        aria-label="Go back"
      >
        <ArrowLeft size={18} />
      </button>
      <div className="flex items-center gap-2">
        <ModeIcon mode={mode} />
        <h3 className="text-base font-medium tracking-tight">
          {config?.title || 'AI Assistant'}
        </h3>
      </div>
      <div className="ml-auto">
        <ConnectionStatus isConnected={isConnected} />
      </div>
    </div>
  );
};

export default ChatHeader;