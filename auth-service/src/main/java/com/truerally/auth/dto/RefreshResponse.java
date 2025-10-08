package com.truerally.auth.dto;

import lombok.AllArgsConstructor;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class RefreshResponse {
    private String message;
    private String email;
    private String accessToken;
    private String refreshToken;
    private long expiresIn; // ms until expiry

    public PublicRefreshResponse toPublicResponse() {
        return new PublicRefreshResponse(message, email, accessToken, expiresIn);
    }
}

