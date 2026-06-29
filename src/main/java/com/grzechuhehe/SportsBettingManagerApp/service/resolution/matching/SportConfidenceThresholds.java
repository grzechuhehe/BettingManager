package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SportConfidenceThresholds {

    private final double defaultThreshold;
    private final double tennisThreshold;

    public SportConfidenceThresholds(
            @Value("${bet.resolution.match-confidence-threshold-default:0.85}") double defaultThreshold,
            @Value("${bet.resolution.match-confidence-threshold-tennis:0.80}") double tennisThreshold) {
        this.defaultThreshold = defaultThreshold;
        this.tennisThreshold = tennisThreshold;
    }

    public double forBet(Bet bet) {
        if (bet != null && bet.getSport() != null && !bet.getSport().isBlank()) {
            String sport = bet.getSport().toLowerCase(Locale.ROOT);
            if (sport.contains("tennis") || sport.contains("tenis")) {
                return tennisThreshold;
            }
        }
        return defaultThreshold;
    }
}
