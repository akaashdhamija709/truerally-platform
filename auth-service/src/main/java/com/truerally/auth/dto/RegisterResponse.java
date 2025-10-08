package com.truerally.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterResponse {
    private String message;
    private String email;
}
