package com.mh.AIAssistant.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAIEmbeddingService {

    private final WebClient webClient;

    public OpenAIEmbeddingService(@Value("${chatgpt.api.url}") String baseUrl,
                                  @Value("${chatgpt.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public List<Double> generateEmbedding(String text) {
        Map<String, Object> request = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> data = ((List<Map<String, Object>>) response.get("data")).get(0);
        return (List<Double>) data.get("embedding");
    }
}

