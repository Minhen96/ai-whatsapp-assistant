import { API_ENDPOINTS } from '../../utils/constants';
import { getApiUrl } from '../../utils/apiHelpers';

export const sendWhatsAppMessage = async (message, from = 'frontend-user') => {
  const endpoint = getApiUrl(API_ENDPOINTS.WHATSAPP);
  
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ from, body: message }).toString()
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Backend error:', errorText);
    throw new Error(`HTTP ${response.status}: ${errorText}`);
  }

  return response.json();
};