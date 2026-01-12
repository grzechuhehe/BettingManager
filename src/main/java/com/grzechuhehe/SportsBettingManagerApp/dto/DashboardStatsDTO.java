package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;

public record DashboardStatsDTO(
    BigDecimal totalProfitLoss,
    Integer totalBets,
    BigDecimal winRate,
    Integer activeBetsCount
) {}
