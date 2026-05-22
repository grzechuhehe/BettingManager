package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrackProfileRequest {
    @NotBlank(message = "Username from X (Twitter) is required")
    @JsonProperty("xUsername")
    private String xUsername;
}
