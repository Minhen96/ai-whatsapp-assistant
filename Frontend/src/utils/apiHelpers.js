export const getApiUrl = (endpoint) => {
    const isDevelopment = process.env.NODE_ENV === 'development';
    return isDevelopment ? endpoint : `http://localhost:8080${endpoint}`;
    };
    
    export const createFetchConfig = (method, body, contentType = 'application/json') => {
    const config = {
        method,
        headers: { 'Content-Type': contentType }
    };
    
    if (body) {
        config.body = contentType === 'application/json' 
        ? JSON.stringify(body) 
        : body;
    }
    
    return config;
};