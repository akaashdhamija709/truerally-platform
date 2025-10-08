// ValidateResetTokenResponse.java
package com.truerally.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateResetTokenResponse {
    private String message;
    private boolean valid;
}
