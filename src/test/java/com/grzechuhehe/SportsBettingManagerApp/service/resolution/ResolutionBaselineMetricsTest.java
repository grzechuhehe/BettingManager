package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Baseline z raportu 2026-06-29 (non-retroactive legs).
 * Po implementacji planu — zaktualizuj oczekiwane progi lub zamień na test integracyjny z DB.
 */
class ResolutionBaselineMetricsTest {

    static final double BASELINE_LEGS_RESOLVED_PCT = 41.2;
    static final double BASELINE_APIFY_RESOLVED_PCT = 28.6;
    static final double BASELINE_ATTEMPT_SUCCESS_PCT = 2.5;
    static final double TARGET_LEGS_RESOLVED_PCT = 50.0;

    @Test
    void documentsBaselineAndTarget() {
        assertTrue(BASELINE_LEGS_RESOLVED_PCT < TARGET_LEGS_RESOLVED_PCT);
        assertEquals(86.0, 86.0, 0.01); // BELOW_THRESHOLD share — główny problem do naprawy
    }
}
