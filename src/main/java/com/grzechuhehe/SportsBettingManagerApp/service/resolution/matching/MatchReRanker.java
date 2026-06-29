package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class MatchReRanker {

    public double adjust(double baseConfidence, Bet bet, SofaScoreEventDto event, int dateWindowDays) {
        double score = baseConfidence;
        score += dateProximityBonus(bet.getPlacedAt(), event.getStartTimestamp(), dateWindowDays);
        score += substringBonus(bet.getEventName(), event.getHomeTeam(), event.getAwayTeam());
        return Math.min(1.0, score);
    }

    private double dateProximityBonus(LocalDateTime placedAt, Long startTimestamp, int dateWindowDays) {
        if (placedAt == null || startTimestamp == null) {
            return 0.0;
        }
        LocalDateTime eventTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimestamp), ZoneOffset.UTC);
        long hours = Math.abs(ChronoUnit.HOURS.between(placedAt, eventTime));
        if (hours <= 24) {
            return 0.10;
        }
        if (hours <= dateWindowDays * 24L) {
            return 0.05;
        }
        return 0.0;
    }

    private double substringBonus(String eventName, String home, String away) {
        if (eventName == null) {
            return 0.0;
        }
        String lower = eventName.toLowerCase(Locale.ROOT);
        double bonus = 0.0;
        if (home != null && lower.contains(home.toLowerCase(Locale.ROOT).split(" ")[0])) {
            bonus += 0.05;
        }
        if (away != null && lower.contains(away.toLowerCase(Locale.ROOT).split(" ")[0])) {
            bonus += 0.05;
        }
        return bonus;
    }
}
