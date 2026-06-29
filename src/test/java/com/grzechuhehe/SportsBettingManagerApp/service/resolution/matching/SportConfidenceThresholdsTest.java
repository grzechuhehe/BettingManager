package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SportConfidenceThresholdsTest {

    private final SportConfidenceThresholds thresholds = new SportConfidenceThresholds(0.85, 0.80);

    @Test
    void tennisGetsLowerThreshold() {
        Bet bet = Bet.builder().sport("Tennis").build();
        assertEquals(0.80, thresholds.forBet(bet));
    }

    @Test
    void footballGetsDefaultThreshold() {
        Bet bet = Bet.builder().sport("Football").build();
        assertEquals(0.85, thresholds.forBet(bet));
    }
}
