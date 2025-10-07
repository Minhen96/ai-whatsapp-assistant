export const createMessage = (content, type = 'user', mode = null, documents = null) => ({
    id: Date.now(),
    type,
    content,
    timestamp: new Date(),
    documents: documents || [],
    ...(mode && { mode })
});

export const createErrorMessage = (error, mode = null) => 
createMessage(
    `Sorry, there was an error: ${error.message}. Please try again.`,
    'bot',
    mode
);

