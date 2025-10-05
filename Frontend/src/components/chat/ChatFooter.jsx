import React from 'react';
import MessageInput from './MessageInput';
import FileUploader from './FileUploader';
import { MODE_CONFIG, CHAT_MODES } from '../../utils/constants';

const ChatFooter = ({ mode, onSendMessage, onFileUpload, isUploading }) => {
  const config = MODE_CONFIG[mode];
  const showFileUploader = mode === CHAT_MODES.STORE;

  return (
    <div className="px-4 md:px-6 py-4 bg-white/70 border-t border-white/40">
      <div className="max-w-2xl mx-auto space-y-3">
        {showFileUploader && (
          <FileUploader 
            onFileUpload={onFileUpload} 
            isUploading={isUploading} 
          />
        )}
        <MessageInput 
          onSendMessage={onSendMessage} 
          placeholder={config?.placeholder || 'Type your message...'} 
        />
      </div>
    </div>
  );
};

export default ChatFooter;