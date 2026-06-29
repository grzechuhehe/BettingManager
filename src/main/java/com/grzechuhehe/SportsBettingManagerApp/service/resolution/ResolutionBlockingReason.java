package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

public enum ResolutionBlockingReason {
    OUTRIGHT_UNSUPPORTED,
    MANUAL_ONLY_SELECTION,
    NOT_SEARCHABLE_EVENT,
    COOLDOWN,
    TOO_RECENT,
    UNSUPPORTED_MARKET
}
