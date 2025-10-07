import React, { useState } from 'react';
import { Download, FileText, File, Image, FileSpreadsheet, Loader2 } from 'lucide-react';
import { useChat } from '../../context/ChatContext.js';

const DocumentList = ({ documents }) => {
  const { userId } = useChat();
  const [downloading, setDownloading] = useState({});

  if (!documents || documents.length === 0) {
    return null;
  }

  // ✅ Filter out duplicate documents (same fileName or filePath)
  const uniqueDocuments = documents.filter(
    (doc, index, self) =>
      index ===
      self.findIndex(
        (d) =>
          (d.filePath && doc.filePath && d.filePath === doc.filePath) ||
          (d.fileName && doc.fileName && d.fileName === doc.fileName)
      )
  );

  const getFileIcon = (fileType) => {
    if (!fileType) return <File className="w-4 h-4" />;
    if (fileType.includes('image')) return <Image className="w-4 h-4" />;
    if (fileType.includes('pdf')) return <FileText className="w-4 h-4" />;
    if (fileType.includes('spreadsheet') || fileType.includes('excel')) {
      return <FileSpreadsheet className="w-4 h-4" />;
    }
    return <File className="w-4 h-4" />;
  };

  const handleDownload = async (docId, fileName) => {
    setDownloading((prev) => ({ ...prev, [docId]: true }));
    try {
      const response = await fetch(`/document/${docId}/download?userId=frontend-user`, {
        method: 'GET',
        headers: { Accept: 'application/octet-stream' },
      });

      if (!response.ok) throw new Error(`Download failed: ${response.status}`);

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'download';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Download error:', error);
      alert('Failed to download file. Please try again.');
    } finally {
      setDownloading((prev) => ({ ...prev, [docId]: false }));
    }
  };

  const formatSimilarity = (similarity) =>
    similarity ? `${(similarity * 100).toFixed(0)}%` : '';

  return (
    <div className="mt-3 space-y-2">
      <div className="text-xs font-medium text-gray-600 mb-1">
        📎 {uniqueDocuments.length}{' '}
        {uniqueDocuments.length === 1 ? 'Document' : 'Documents'}
      </div>

      {uniqueDocuments.map((doc) => (
        <div
          key={doc.id}
          className="bg-gray-50 rounded-lg p-2.5 border border-gray-200 text-sm"
        >
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1 min-w-0">
              {doc.hasFile && doc.fileName && (
                <div className="flex items-center gap-1.5 mb-1.5">
                  <span className="text-gray-500">{getFileIcon(doc.fileType)}</span>
                  <span className="font-medium text-gray-900 truncate text-xs">
                    {doc.fileName}
                  </span>
                  {doc.similarity && (
                    <span className="text-[10px] px-1.5 py-0.5 bg-green-100 text-green-700 rounded">
                      {formatSimilarity(doc.similarity)} match
                    </span>
                  )}
                </div>
              )}

              <p className="text-xs text-gray-600 line-clamp-2">
                {doc.content?.substring(0, 150)}
                {doc.content?.length > 150 && '...'}
              </p>
            </div>

            {doc.hasFile && (
              <button
                onClick={() => handleDownload(doc.id, doc.fileName)}
                disabled={downloading[doc.id]}
                className="flex items-center gap-1 px-2 py-1 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white rounded text-xs transition-colors shrink-0"
                title="Download file"
              >
                {downloading[doc.id] ? (
                  <Loader2 className="w-3 h-3 animate-spin" />
                ) : (
                  <Download className="w-3 h-3" />
                )}
                <span className="hidden sm:inline">Download</span>
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default DocumentList;
