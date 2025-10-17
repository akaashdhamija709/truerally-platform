// ConfirmResetRequest.java
package com.truerally.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmResetRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}
