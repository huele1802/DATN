package com.example.AI.Hotel.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotEmpty(message = "MISSING_REQUIRED_FIELDS: Email cannot be empty")
    @Email(message = "INVALID_EMAIL_FORMAT: Email must be a valid email address")
    private String email;

    @NotEmpty(message = "MISSING_REQUIRED_FIELDS: Password cannot be empty")
    @Size(min = 6, message = "INVALID_PASSWORD_FORMAT: Password must be at least 6 characters")
    private String password;
}
