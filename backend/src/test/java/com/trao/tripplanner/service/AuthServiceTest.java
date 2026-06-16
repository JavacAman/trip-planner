package com.trao.tripplanner.service;

import com.trao.tripplanner.dto.request.LoginRequest;
import com.trao.tripplanner.dto.request.RegisterRequest;
import com.trao.tripplanner.dto.response.AuthResponse;
import com.trao.tripplanner.exception.EmailAlreadyExistsException;
import com.trao.tripplanner.model.User;
import com.trao.tripplanner.repository.UserRepository;
import com.trao.tripplanner.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");

        savedUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encoded_password")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("register: successfully registers a new user and returns JWT token")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("jwt_token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register: throws EmailAlreadyExistsException when email is taken")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: returns token on valid credentials")
    void login_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(savedUser, null, savedUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtTokenProvider.generateToken(any(org.springframework.security.core.Authentication.class))).thenReturn("jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt_token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("login: throws BadCredentialsException on wrong password")
    void login_badCredentials_throwsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
