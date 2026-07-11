package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import java.math.BigDecimal;

public record BetStakeNormalizationResult(
        BigDecimal stake,
        BigDecimal units,
        String currency
) {
}
