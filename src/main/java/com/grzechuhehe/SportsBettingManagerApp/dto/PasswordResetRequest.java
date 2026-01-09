package com.grzechuhehe.SportsBettingManagerApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Nieprawidłowy format email")
    private String email;
}