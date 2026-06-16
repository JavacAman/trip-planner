package com.trao.tripplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trao.tripplanner.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// SOLID-SRP: Handles only communication with the Grok (xAI) API
// Pattern-Singleton: @Service produces a single Spring-managed instance
@Service
@Slf4j
public class GrokAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.grok.model}")
    private String model;

    @Value("${app.grok.max-tokens}")
    private int maxTokens;

    public GrokAiService(@Value("${app.grok.api-url}") String apiUrl,
                         @Value("${app.grok.api-key}") String apiKey,
                         ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Sends a prompt to Grok and returns the raw text response.
     * The caller is responsible for parsing the structured JSON response.
     */
    public String generateContent(String prompt) {
        log.debug("Sending prompt to Grok API, length={}", prompt.length());

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an expert travel planner. Always respond with valid JSON only. No markdown code blocks, no explanations."),
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extractContentFromResponse(response);

        } catch (WebClientResponseException ex) {
            log.error("Grok API HTTP error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AiServiceException("Grok API returned error: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Grok API call failed: {}", ex.getMessage());
            throw new AiServiceException("Failed to communicate with AI service", ex);
        }
    }

    private String extractContentFromResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Strip markdown code fences if model adds them despite the system prompt
            return content
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
        } catch (Exception ex) {
            log.error("Failed to parse Grok API response: {}", ex.getMessage());
            throw new AiServiceException("Failed to parse AI response");
        }
    }
}
