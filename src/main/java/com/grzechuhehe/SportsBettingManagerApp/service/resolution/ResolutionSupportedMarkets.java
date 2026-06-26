package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;

import java.util.Set;

/** Rynki rozliczane automatycznie przez warstwę MarketResolver. */
final class ResolutionSupportedMarkets {

    static final Set<MarketType> VALUES = Set.of(
            MarketType.MONEYLINE_1X2,
            MarketType.MONEYLINE_12,
            MarketType.TOTALS_OVER_UNDER,
            MarketType.BOTH_TEAMS_TO_SCORE,
            MarketType.CORRECT_SCORE,
            MarketType.HANDICAP,
            MarketType.ASIAN_HANDICAP
    );

    private ResolutionSupportedMarkets() {}
}
