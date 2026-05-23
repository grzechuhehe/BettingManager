package com.grzechuhehe.SportsBettingManagerApp.dto.profile;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePickDTO {
    private Long id;
    private BetType betType;
    private String eventName;
    private String selection;
    private BigDecimal odds;
    private BigDecimal units;
    private String bookmaker;
    private BetStatus status;
    private String imageProofPath;
    private LocalDateTime placedAt;
    private String sourcePostId;
    private List<ProfilePickDTO> legs;
}
