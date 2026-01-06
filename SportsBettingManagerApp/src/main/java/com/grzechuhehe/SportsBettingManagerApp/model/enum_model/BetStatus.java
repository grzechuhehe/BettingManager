package com.grzechuhehe.SportsBettingManagerApp.model.enum_model;

public enum BetStatus {
    PENDING,    // Zakład w toku, czeka na rozstrzygnięcie
    WON,        // Zakład wygrany
    LOST,       // Zakład przegrany
    VOID,       // Zakład anulowany/zwrócony (np. z powodu nieodbycia się meczu)
    CASHED_OUT, // Zakład zamknięty przed czasem przez użytkownika
    HALF_WON,   // Zakład częściowo wygrany (częste w zakładach azjatyckich)
    HALF_LOST   // Zakład częściowo przegrany (częste w zakładach azjatyckich)
}
