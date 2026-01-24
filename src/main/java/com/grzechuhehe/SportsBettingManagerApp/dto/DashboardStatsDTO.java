package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardStatsDTO(
    BigDecimal totalProfitLoss,
    Integer totalBets,
    BigDecimal winRate,
    Integer activeBetsCount,
    BigDecimal roi,
    BigDecimal yield,
    BigDecimal totalStaked,
    Map<String, BigDecimal> profitBySport,
    List<EquityCurvePoint> equityCurve
) {}