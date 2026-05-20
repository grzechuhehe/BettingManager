package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;
import java.util.List;

public record UnifiedMarketData(
    BigDecimal blendedProbability,
    BigDecimal totalOpenInterest,
    List<String> sources
) {}
