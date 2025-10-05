import { API_ENDPOINTS } from '../../utils/constants';
import { getApiUrl, createFetchConfig } from '../../utils/apiHelpers';

export const sendChatMessage = async (message, mode, userId = 'frontend-user') => {
  const endpoint = getApiUrl(API_ENDPOINTS.CHAT);
  const config = createFetchConfig('POST', { message, mode, userId });

  const response = await fetch(endpoint, config);

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Backend error:', errorText);
    throw new Error(`HTTP ${response.status}: ${errorText}`);
  }

  return response.json();
};