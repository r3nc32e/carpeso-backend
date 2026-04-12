package com.carpeso.carpeso_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String email;
    private String fullName;
    private Long userId;
    private Set<String> privileges;
    private boolean otpRequired;
    private String otpCode;
}