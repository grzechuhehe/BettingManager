package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BetResponse {
    private Long id;
    private BetType betType;
    private BetStatus status;
    private BigDecimal stake;
    private BigDecimal odds;
    private OddsType oddsType;
    private BigDecimal potentialWinnings;
    private BigDecimal finalProfit;

    private String sport;
    private String eventName;
    private LocalDateTime eventDate;

    private MarketType marketType;
    private String selection;
    private String line;

    private String bookmaker;
    private String externalBetId;
    private String externalApiName;

    private LocalDateTime placedAt;
    private LocalDateTime settledAt;

    private String notes;

    private Long userId;
    private List<BetResponse> childBets; // Dla zakładów PARLAY
}
