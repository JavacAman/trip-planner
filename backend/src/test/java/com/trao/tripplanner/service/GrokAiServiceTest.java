package com.trao.tripplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trao.tripplanner.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrokAiService Unit Tests")
class GrokAiServiceTest {

    private GrokAiService grokAiService;

    @BeforeEach
    void setUp() {
        // Points at a non-routable local port (nothing listens here) so the connection fails
        // immediately and deterministically — this test must never depend on a live AI API.
        grokAiService = new GrokAiService(
                "http://localhost:1",
                "test-api-key",
                new ObjectMapper()
        );
        // Inject private fields via reflection for testing
        try {
            var modelField = GrokAiService.class.getDeclaredField("model");
            modelField.setAccessible(true);
            modelField.set(grokAiService, "llama-3.3-70b-versatile");

            var maxTokensField = GrokAiService.class.getDeclaredField("maxTokens");
            maxTokensField.setAccessible(true);
            maxTokensField.setInt(grokAiService, 4096);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("generateContent: throws AiServiceException when the API call fails")
    void generateContent_connectionFailure_throwsAiServiceException() {
        assertThatThrownBy(() -> grokAiService.generateContent("test prompt"))
                .isInstanceOf(AiServiceException.class);
    }
}
