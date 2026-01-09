package com.grzechuhehe.SportsBettingManagerApp.model.enum_model;

public enum MarketType {
    // Piłka nożna, Koszykówka, etc.
    MONEYLINE_1X2,      // Zwycięzca meczu (1 - gospodarz, X - remis, 2 - gość)
    MONEYLINE_12,       // Zwycięzca meczu (bez remisu)

    // Zakłady na sumy
    TOTALS_OVER_UNDER,  // Powyżej/Poniżej (np. liczba goli, punktów)

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
