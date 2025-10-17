package com.truerally.auth.dto;

import lombok.AllArgsConstructor;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class LoginResponse {
    private String message;
    private String email;
    private String accessToken;
    private String refreshToken;
    private long expiresIn; // ms until expiry

    public PublicLoginResponse toPublicResponse() {
        return new PublicLoginResponse(message, email, accessToken, expiresIn);
    }
}

