import { API_ENDPOINTS } from '../../utils/constants';
import { getApiUrl } from '../../utils/apiHelpers';

export const uploadFile = async (file, userId = 'frontend-user') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('userId', userId);

  const endpoint = getApiUrl(API_ENDPOINTS.KNOWLEDGE_STORE);

  const response = await fetch(endpoint, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Upload error:', errorText);
    throw new Error(`HTTP ${response.status}: ${errorText}`);
  }

  return response.json();
};