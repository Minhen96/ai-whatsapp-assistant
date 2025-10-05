import React, { useRef, useState } from 'react';

function FileUploader({ onFileUpload, isUploading }) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState(null);

  const onChoose = () => inputRef.current?.click();
  const onChange = (e) => setFile(e.target.files?.[0] || null);

  const onDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFile(e.dataTransfer.files[0]);
    }
  };

  const onUpload = async () => {
    if (file && !isUploading) {
      await onFileUpload(file);
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div>
      {!file ? (
        <div
          className={`rounded-xl border border-gray-200 bg-white/80 p-4 text-center transition ${dragging ? 'ring-2 ring-brand' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          onClick={onChoose}
        >
          <input ref={inputRef} type="file" onChange={onChange} className="hidden" accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png" />
          <div className="text-sm text-gray-700">Click to upload or drag and drop</div>
          <div className="text-xs text-gray-500 mt-1">PDF, DOC, TXT, or images (max 10MB)</div>
        </div>
      ) : (
        <div className="flex items-center justify-between rounded-xl border border-gray-200 bg-white/80 p-3">
          <div className="text-sm text-gray-700 truncate mr-3">{file.name}</div>
          <div className="flex items-center gap-2">
            <button onClick={onUpload} disabled={isUploading} className="px-3 py-1.5 rounded-lg bg-brand text-white text-sm disabled:opacity-60">{isUploading ? 'Uploading...' : 'Upload'}</button>
            <button onClick={() => { setFile(null); if (inputRef.current) inputRef.current.value = ''; }} className="px-3 py-1.5 rounded-lg bg-gray-100 text-gray-700 text-sm">Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
}

export default FileUploader;


