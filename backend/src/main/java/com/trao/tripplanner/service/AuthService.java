package com.trao.tripplanner.service;

import com.trao.tripplanner.dto.request.LoginRequest;
import com.trao.tripplanner.dto.request.RegisterRequest;
import com.trao.tripplanner.dto.response.AuthResponse;
import com.trao.tripplanner.exception.EmailAlreadyExistsException;
import com.trao.tripplanner.model.User;
import com.trao.tripplanner.repository.UserRepository;
import com.trao.tripplanner.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// SOLID-SRP: Handles only authentication and registration business logic
// ACID-Atomicity: @Transactional ensures registration either fully completes or fully rolls back
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // ACID-Atomicity: Rolls back if any step (save, hash) fails
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Pattern-Builder: Fluent builder for User construction with defaults handled by @Builder.Default
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: email={}", user.getEmail());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(authentication);

        log.info("User logged in: email={}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    // Pattern-Factory: Centralizes construction of AuthResponse to avoid duplication
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
