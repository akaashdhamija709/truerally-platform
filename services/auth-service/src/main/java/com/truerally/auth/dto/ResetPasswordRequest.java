package com.truerally.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @Email
    private String email;
}
