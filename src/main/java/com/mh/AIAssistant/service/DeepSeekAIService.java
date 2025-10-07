package com.mh.AIAssistant.service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeepSeekAIService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAIService.class);

    private final WebClient webClient;

    @Value("${deepseek.api.key}")
    private String deepSeekApiKey;

    @Value("${deepseek.chat.model:deepseek-chat}")
    private String chatModel;

    // In-memory conversation history per user
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    
    // Maximum messages to keep in history (to avoid token limits)
    private static final int MAX_HISTORY_SIZE = 10;

    public DeepSeekAIService(@Value("${deepseek.api.url}") String baseUrl,
                             @Value("${deepseek.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String classifyIntent(String userId, String userMessage) {
        String systemPrompt = """
            You are an intent classifier. Analyze the user's message and determine if they want to:
            1. RETRIEVE - They want to see/find/retrieve specific documents from their knowledge base
            2. CHAT - They want to have a conversation or ask questions about the content
            
            Respond with ONLY one word: either "RETRIEVE" or "CHAT"
            
            RETRIEVE examples:
            - "Show me the document about project deadlines"
            - "Find my notes on machine learning"
            - "Get me the file I uploaded yesterday"
            - "What documents do I have about budget?"
            - "Retrieve the meeting notes from last week"
            
            CHAT examples:
            - "What is machine learning?"
            - "Explain the project deadline"
            - "How does this work?"
            - "Can you summarize this?"
            - "Tell me about the budget"
            """;
        
        try {
            String fullPrompt = systemPrompt + "\n\nUser message: " + userMessage;
            String response = chat(userId, fullPrompt).trim().toUpperCase();
            
            // Ensure we only get RETRIEVE or CHAT
            if (response.contains("RETRIEVE")) {
                return "RETRIEVE";
            }
            return "CHAT";
        } catch (Exception e) {
            System.err.println("Error classifying intent, defaulting to CHAT: " + e.getMessage());
            return "CHAT";
        }
    }

    /**
     * Chat with knowledge base context and conversation memory
     */
    public String chatWithKnowledge(String userId, String userQuestion, List<String> contextTexts) {
        // Build system prompt with knowledge base context
        String systemPrompt = buildSystemPrompt(contextTexts);
        
        // Get or create conversation history for this user
        List<Map<String, String>> history = conversationHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        
        // Build messages list: system + history + new user message
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userQuestion));

        Map<String, Object> request = Map.of(
            "model", chatModel,
            "messages", messages,
            "stream", false,
            "temperature", 0.7,
            "max_tokens", 2000
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                            .filter(ex -> !(ex instanceof java.util.concurrent.TimeoutException)))
                    .block();

            if (response == null) return fallbackMessage();

            String assistantReply = extractReply(response);
            
            // Save to conversation history
            addToHistory(userId, userQuestion, assistantReply);
            
            return assistantReply;
            
        } catch (Exception ex) {
            System.err.println("DeepSeek API error: " + ex.getMessage());
            return fallbackMessage();
        }
    }

    /**
     * Simple chat without knowledge base (for general questions)
     */
    public String chat(String userId, String userQuestion) {
        return chatWithKnowledge(userId, userQuestion, Collections.emptyList());
    }

    /**
     * Clear conversation history for a user
     */
    public void clearHistory(String userId) {
        conversationHistory.remove(userId);
    }

    /**
     * Build an improved system prompt
     */
    private String buildSystemPrompt(List<String> contextTexts) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful AI assistant. ");
        
        if (!contextTexts.isEmpty()) {
            prompt.append("Use the following knowledge base to answer questions accurately. ");
            prompt.append("If the answer is in the knowledge base, cite it. ");
            prompt.append("If not, use your general knowledge but mention that it's not from the user's knowledge base.\n\n");
            prompt.append("=== KNOWLEDGE BASE ===\n");
            
            // Limit context to avoid token overflow
            int maxChars = 8000;
            int currentChars = 0;
            
            for (String context : contextTexts) {
                if (currentChars + context.length() > maxChars) {
                    prompt.append("\n[Additional context truncated to fit token limits]\n");
                    break;
                }
                prompt.append(context).append("\n\n");
                currentChars += context.length();
            }
            
            prompt.append("=== END KNOWLEDGE BASE ===\n\n");
        }
        
        prompt.append("Respond naturally and conversationally. ");
        prompt.append("Be concise but thorough. ");
        prompt.append("Format your responses in plain text without excessive markdown symbols.");
        
        return prompt.toString();
    }

    /**
     * Extract assistant's reply from API response
     */
    private String extractReply(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                
                // Try "message.content" first (standard format)
                Object messageObj = firstChoice.get("message");
                if (messageObj instanceof Map) {
                    Map<String, Object> message = (Map<String, Object>) messageObj;
                    Object content = message.get("content");
                    if (content instanceof String) {
                        return cleanResponse((String) content);
                    }
                }
                
                // Fallback to "text" field
                Object text = firstChoice.get("text");
                if (text instanceof String) {
                    return cleanResponse((String) text);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
        }
        
        return fallbackMessage();
    }

    /**
     * Clean up the response - remove excessive markdown symbols
     */
    private String cleanResponse(String response) {
        if (response == null) return fallbackMessage();
        
        // Remove excessive backticks and quotes
        String cleaned = response.trim();
        
        // Remove leading/trailing triple backticks
        cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "");
        cleaned = cleaned.replaceAll("\\n?```$", "");
        
        // Remove excessive quotes at start/end
        if (cleaned.startsWith("\"\"\"") && cleaned.endsWith("\"\"\"")) {
            cleaned = cleaned.substring(3, cleaned.length() - 3).trim();
        }
        
        return cleaned;
    }

    /**
     * Add exchange to conversation history with size limit
     */
    private void addToHistory(String userId, String userMessage, String assistantReply) {
        List<Map<String, String>> history = conversationHistory.get(userId);
        if (history == null) return;
        
        // Add user message and assistant reply
        history.add(Map.of("role", "user", "content", userMessage));
        history.add(Map.of("role", "assistant", "content", assistantReply));
        
        // Keep only last N messages (each exchange is 2 messages)
        while (history.size() > MAX_HISTORY_SIZE * 2) {
            history.remove(0);
            history.remove(0);
        }
    }

    private String fallbackMessage() {
        return "I'm having trouble reaching the AI service right now. Please try again shortly.";
    }
}