package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import java.math.BigDecimal;

public record MarketData(
        BigDecimal probability,
        BigDecimal openInterest
) {}
