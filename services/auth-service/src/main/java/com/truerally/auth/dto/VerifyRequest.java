package com.truerally.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {
    @NotBlank(message = "Verification token is required")
    private String token;

    public VerifyRequest(String token) {
        this.token=token;
    }
}
