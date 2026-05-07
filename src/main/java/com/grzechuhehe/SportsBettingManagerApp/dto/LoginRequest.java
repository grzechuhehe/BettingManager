package com.grzechuhehe.SportsBettingManagerApp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @Schema(example = "trader_zero", description = "User identifier")
    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    private String username;

    @Schema(example = "secret_key_123", description = "Account access key")
    @NotBlank(message = "Hasło jest wymagane")
    private String password;
}