package com.grzechuhehe.SportsBettingManagerApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetSubmit {
    @NotBlank(message = "Token jest wymagany")
    private String token;

    @NotBlank(message = "Nowe hasło jest wymagane")
    @Size(min = 6, message = "Hasło musi mieć minimum 6 znaków")
    private String newPassword;
}