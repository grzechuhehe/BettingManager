package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePreviewDTO {
    @JsonProperty("xUsername")
    private String xUsername;
    @JsonProperty("xProfileUrl")
    private String xProfileUrl;
    private boolean tracked;
    private LocalDateTime trackedSince;
    private LocalDateTime lastXCheckAt;
    private Integer totalBets;
    private BigDecimal winRate;
    private BigDecimal yield;
}
