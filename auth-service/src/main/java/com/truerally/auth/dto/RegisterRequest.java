/*package com.truerally.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String fullName;

    @NotBlank
    private String gender;

    @NotBlank
    private String country;

    @NotBlank
    private String city;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotBlank
    private String primaryRole; // PLAYER, COACH, ORGANIZER
}*/


package com.truerally.auth.dto;

import lombok.*;
        import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private LocalDate dob;
    private String country;
    private String pincode;
    private String gender;
}
