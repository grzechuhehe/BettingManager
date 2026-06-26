package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;

import java.util.List;

/**
 * Wynik batch search — odróżnia błąd HTTP/timeout (successful=false) od pustej odpowiedzi actora.
 */
public record ApifyBatchResult(List<SofaScoreEventDto> matches, boolean successful) {

    public static ApifyBatchResult success(List<SofaScoreEventDto> matches) {
        return new ApifyBatchResult(matches == null ? List.of() : matches, true);
    }

    public static ApifyBatchResult failure() {
        return new ApifyBatchResult(List.of(), false);
    }
}
