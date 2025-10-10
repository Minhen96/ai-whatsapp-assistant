export const getApiUrl = (endpoint) => {
  const baseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
  return `${baseUrl}${endpoint}`;
};

export const getWsUrl = (endpoint) => {
  const baseUrl = process.env.REACT_APP_WS_BASE_URL || 'ws://localhost:8080';
  return `${baseUrl}${endpoint}`;
};

export const createFetchConfig = (method, body, contentType = 'application/json') => {
  const config = {
    method,
    headers: { 'Content-Type': contentType },
  };

  if (body) {
    config.body =
      contentType === 'application/json' ? JSON.stringify(body) : body;
  }

  return config;
};
