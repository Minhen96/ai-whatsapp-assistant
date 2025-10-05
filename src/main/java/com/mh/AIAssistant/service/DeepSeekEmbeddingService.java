package com.mh.AIAssistant.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class DeepSeekEmbeddingService {

    private final WebClient webClient;
    private final String apiKey = "sk-your-deepseek-api-key"; // Replace with your actual API key
    private final String apiUrl = "https://api.deepseek.com/v1/embeddings";

    public DeepSeekEmbeddingService() {
        this.webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }

    public List<Double> generateEmbedding(String text) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", "deepseek-embedding");

            Map<String, Object> response = webClient.post()
                .uri("")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }

            throw new RuntimeException("Failed to generate embedding");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating embedding: " + e.getMessage());
        }
    }
}