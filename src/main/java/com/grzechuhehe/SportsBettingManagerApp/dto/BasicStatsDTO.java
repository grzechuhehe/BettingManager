package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;
import java.util.List;

public record BasicStatsDTO(
    Integer totalBets,
    Long wonBets,
    BigDecimal totalStake,
    BigDecimal profitLoss,
    BigDecimal roi,
    List<BetResponse> recentBets,
    String displayCurrency
) {}
