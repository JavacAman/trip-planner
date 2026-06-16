package com.trao.tripplanner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Pattern-Factory: AuthResponseFactory produces this DTO via factory method
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
}
