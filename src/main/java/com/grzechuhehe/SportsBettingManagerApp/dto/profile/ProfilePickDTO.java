package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProfilePickDTO {
    private Long id;
    private String eventName;
    private String selection;
    private BigDecimal odds;
    private BigDecimal units;
    private String bookmaker;
    private BetStatus status;
    private String imageProofPath;
    private LocalDateTime placedAt;
    private String sourcePostId;
}
