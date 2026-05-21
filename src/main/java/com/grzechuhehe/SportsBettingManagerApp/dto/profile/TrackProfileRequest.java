package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrackProfileRequest {
    @NotBlank(message = "Username from X (Twitter) is required")
    private String xUsername;
}
