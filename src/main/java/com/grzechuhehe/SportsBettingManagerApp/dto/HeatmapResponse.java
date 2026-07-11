package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;
import java.util.Map;

public record HeatmapResponse(
        String displayCurrency,
        Map<String, BigDecimal> dailyProfit
) {}
