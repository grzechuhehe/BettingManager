package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackedProfileDTO {
    private Long id;
    
    @JsonProperty("xUsername")
    private String xUsername;
    
    @JsonProperty("xProfileUrl")
    private String xProfileUrl;
    
    private LocalDateTime lastXCheckAt;
}

