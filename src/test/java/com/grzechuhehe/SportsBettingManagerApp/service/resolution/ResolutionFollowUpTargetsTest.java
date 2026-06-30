package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionFollowUpTargetsTest {

    static final int PENDING_MONEYLINE_1X2_BASELINE = 31;
    static final int PENDING_BET_BUILDERS_BASELINE = 16;
    static final int MIN_SUCCESS_PER_24H_WHEN_BACKLOG_HIGH = 3;
    static final int BACKLOG_HIGH_THRESHOLD = 50;

    @Test
    void documentsFollowUpTargets() {
        assertTrue(PENDING_MONEYLINE_1X2_BASELINE > 0);
        assertTrue(PENDING_BET_BUILDERS_BASELINE > 0);
        assertTrue(BACKLOG_HIGH_THRESHOLD > MIN_SUCCESS_PER_24H_WHEN_BACKLOG_HIGH);
    }
}
