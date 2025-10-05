import React from 'react';

const ConnectionStatus = ({ isConnected }) => {
  return (
    <div className="flex items-center gap-2 text-sm text-gray-600">
      <span 
        className={`inline-block w-2 h-2 rounded-full ${
          isConnected ? 'bg-green-500' : 'bg-gray-300'
        } animate-pulse`}
        title={isConnected ? 'Connected to server' : 'Disconnected from server'}
      />
      {isConnected ? 'Connected' : 'Disconnected'}
    </div>
  );
};

export default ConnectionStatus;