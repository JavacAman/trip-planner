package com.trao.tripplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trao.tripplanner.dto.request.LoginRequest;
import com.trao.tripplanner.dto.request.RegisterRequest;
import com.trao.tripplanner.dto.response.AuthResponse;
import com.trao.tripplanner.exception.EmailAlreadyExistsException;
import com.trao.tripplanner.security.JwtAuthenticationFilter;
import com.trao.tripplanner.security.JwtTokenProvider;
import com.trao.tripplanner.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsService userDetailsService;

    private final AuthResponse sampleResponse = AuthResponse.builder()
            .token("test_token")
            .tokenType("Bearer")
            .userId(1L)
            .email("user@example.com")
            .fullName("Test User")
            .role("USER")
            .build();

    @Test
    @DisplayName("POST /auth/register: returns 201 with token on valid request")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(authService.register(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("test_token"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register: returns 400 when email is invalid")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/register: returns 409 when email is already taken")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("taken@example.com");
        request.setPassword("password123");

        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("taken@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/login: returns 200 with token on valid credentials")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("test_token"));
    }

    @Test
    @DisplayName("POST /auth/login: returns 400 when body is missing email")
    void login_missingEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
