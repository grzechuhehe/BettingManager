package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquityCurvePoint(
    LocalDate date,
    BigDecimal cumulativeProfit
) {}
