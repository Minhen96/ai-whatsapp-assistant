import { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { createMessage, createErrorMessage } from '../utils/messageHelpers';
import { uploadFile } from '../services/api/knowledgeApi';

export const useFileUpload = () => {
  const { addMessage } = useChat();
  const [isUploading, setIsUploading] = useState(false);

  const handleFileUpload = async (file) => {
    setIsUploading(true);

    try {
      const data = await uploadFile(file);
      
      const successMessage = createMessage(
        data.message || 'File uploaded successfully!',
        'bot',
        'store'
      );
      addMessage(successMessage);

    } catch (error) {
      console.error('Error uploading file:', error);
      const errorMessage = createMessage(
        `Sorry, there was an error uploading your file: ${error.message}`,
        'bot',
        'store'
      );
      addMessage(errorMessage);
    } finally {
      setIsUploading(false);
    }
  };

  return { handleFileUpload, isUploading };
};