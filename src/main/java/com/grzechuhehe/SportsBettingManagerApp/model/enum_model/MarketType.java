package com.grzechuhehe.SportsBettingManagerApp.model.enum_model;

public enum MarketType {
    // Piłka nożna, Koszykówka, etc.
    MONEYLINE_1X2,      // Zwycięzca meczu (1 - gospodarz, X - remis, 2 - gość)
    MONEYLINE_12,       // Zwycięzca meczu (bez remisu)

    // Zakłady na sumy
    TOTALS_OVER_UNDER,  // Powyżej/Poniżej (np. liczba goli, punktów)
    /** Alias używany przez Gemini w BetBuilder (synonim TOTALS_OVER_UNDER). */
    OVER_UNDER,
    /** Gole konkretnej drużyny (np. „Senegal Over 1.5”). */
    TEAM_TOTAL_GOALS,
    /** Synonim TEAM_TOTAL_GOALS z Gemini. */
    TEAM_TOTAL_GOALS_OVER_UNDER,
    /** Strzały konkretnej drużyny — wymaga statystyk meczu. */
    TEAM_TOTAL_SHOTS,
    /** Kartki w meczu — wymaga statystyk meczu. */
    TOTAL_CARDS_OVER_UNDER,
    /** Podwójna szansa: 1X, X2, 12. */
    DOUBLE_CHANCE,
    /** Zwycięzca meczu (alias Gemini → MONEYLINE_1X2). */
    MATCH_ODDS,

    // Player props (wymagają statystyk / incidents z Apify enrichment)
    PLAYER_TOTAL_SHOTS,
    PLAYER_SHOTS,
    PLAYER_SHOTS_ON_TARGET,
    PLAYER_FOULS,
    PLAYER_FOULS_COMMITTED_AGAINST,

    // Rogi — wymagają statystyk meczu
    CORNERS_HEAD_TO_HEAD,
    CORNER_1X2,

    // Handicapy
    HANDICAP,           // Handicap europejski
    ASIAN_HANDICAP,     // Handicap azjatycki

    // Zakłady specjalne
    CORRECT_SCORE,      // Dokładny wynik
    PLAYER_PROPS,       // Zakłady na statystyki graczy
    BOTH_TEAMS_TO_SCORE,

    // Rynki predykcyjne / Giełdy zakładów
    PREDICTION_MARKET,  // Ogólny rynek predykcyjny

    // Inne
    OUTRIGHT,           // Zakład długoterminowy (np. zwycięzca ligi)
    OTHER               // Inny, nieokreślony
}
