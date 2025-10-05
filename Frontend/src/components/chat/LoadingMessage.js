import React from 'react';

function LoadingMessage() {
  return (
    <div className="flex justify-start">
      <div className="rounded-2xl rounded-bl-md border border-gray-200 bg-white px-4 py-2 shadow-sm">
        <div className="flex items-center gap-1">
          <span className="inline-block w-2 h-2 rounded-full bg-gray-400 animate-pulse"></span>
          <span className="inline-block w-2 h-2 rounded-full bg-gray-400 animate-pulse [animation-delay:.15s]"></span>
          <span className="inline-block w-2 h-2 rounded-full bg-gray-400 animate-pulse [animation-delay:.3s]"></span>
        </div>
      </div>
    </div>
  );
}

export default LoadingMessage;
