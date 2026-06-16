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
        grokAiService = new GrokAiService(
                "https://api.x.ai/v1",
                "test-api-key",
                new ObjectMapper()
        );
        // Inject private fields via reflection for testing
        try {
            var modelField = GrokAiService.class.getDeclaredField("model");
            modelField.setAccessible(true);
            modelField.set(grokAiService, "grok-beta");

            var maxTokensField = GrokAiService.class.getDeclaredField("maxTokens");
            maxTokensField.setAccessible(true);
            maxTokensField.setInt(grokAiService, 4096);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("generateContent: throws AiServiceException when API returns HTTP error")
    void generateContent_networkError_throwsAiServiceException() {
        // WebClient will fail immediately because "test-api-key" is invalid
        // but we verify the exception type is correct
        assertThatThrownBy(() -> grokAiService.generateContent("test prompt"))
                .isInstanceOf(AiServiceException.class);
    }
}
