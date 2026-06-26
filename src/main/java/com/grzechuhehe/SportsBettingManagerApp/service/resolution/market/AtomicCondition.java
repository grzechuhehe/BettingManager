package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;

public record AtomicCondition(
        MarketType marketType,
        String selection,
        String line
) {}
