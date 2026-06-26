package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtomicCondition(
        MarketType marketType,
        String selection,
        String line
) {}
