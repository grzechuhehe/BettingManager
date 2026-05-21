package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

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
    private String xUsername;
    private String xProfileUrl;
    private LocalDateTime lastXCheckAt;
}

