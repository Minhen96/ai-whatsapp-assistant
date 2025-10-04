package com.mh.AIAssistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekAIService {

    private final WebClient webClient;

    @Value("${deepseek.api.key}")
    private String deepSeekApiKey;

    public DeepSeekAIService(@Value("${deepseek.api.url}") String baseUrl,
                             @Value("${deepseek.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl) // e.g., "https://api.deepseek.com"
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String chatWithKnowledge(String userQuestion, List<String> contextTexts) {
        String prompt = "Context:\n" + String.join("\n", contextTexts) +
                        "\n\nUser question: " + userQuestion;

        Map<String, Object> request = Map.of(
            "model", "deepseek-chat",
            "messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)
            ),
            "stream", false
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Object> choices = (List<Object>) response.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
            // Depending on API, could be "text" or "message.content"
            return (String) firstChoice.getOrDefault("text", ((Map<String, Object>) firstChoice.get("message")).get("content"));
        }
        return null;
    }

}
