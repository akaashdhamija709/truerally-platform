package com.truerally.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicLoginResponse {
    private String message;
    private String email;
    private String accessToken;
    private long expiresIn;
}
