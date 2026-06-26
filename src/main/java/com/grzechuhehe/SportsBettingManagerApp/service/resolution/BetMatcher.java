package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class BetMatcher {

    private static final Set<String> STOPWORDS = Set.of("vs", "the", "club");

    private final ResolutionNameTranslator nameTranslator;

    public BetMatcher(ResolutionNameTranslator nameTranslator) {
        this.nameTranslator = nameTranslator;
    }

    public record MatchCandidate(SofaScoreEventDto event, double confidence) {}

    public Optional<MatchCandidate> findBestMatch(Bet bet, List<SofaScoreEventDto> events, int dateWindowDays) {
        if (events == null || events.isEmpty() || bet.getEventName() == null) {
            return Optional.empty();
        }
        Set<String> betTokens = nameTranslator.matchingTokens(
                bet.getEventName(), tokenize(bet.getEventName()));
        if (betTokens.isEmpty()) {
            return Optional.empty();
        }

        MatchCandidate best = null;
        for (SofaScoreEventDto event : events) {
            if (!withinDateWindow(bet.getPlacedAt(), event.getStartTimestamp(), dateWindowDays)) {
                continue;
            }
            Set<String> eventTokens = new HashSet<>();
            eventTokens.addAll(tokenize(event.getHomeTeam()));
            eventTokens.addAll(tokenize(event.getAwayTeam()));
            if (eventTokens.isEmpty()) {
                continue;
            }
            double confidence = jaccard(betTokens, eventTokens);
            if (best == null || confidence > best.confidence()) {
                best = new MatchCandidate(event, confidence);
            }
        }
        return Optional.ofNullable(best);
    }

    boolean withinDateWindow(LocalDateTime placedAt, Long startTimestamp, int dateWindowDays) {
        if (placedAt == null || startTimestamp == null) {
            return true;
        }
        LocalDateTime eventTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimestamp), ZoneOffset.UTC);
        return !eventTime.isBefore(placedAt.minusDays(1))
                && !eventTime.isAfter(placedAt.plusDays(dateWindowDays));
    }

    Set<String> tokenize(String text) {
        if (text == null) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ł", "l") // NFD nie rozkłada ł
                .replaceAll("[^a-z0-9 ]", " ");
        Set<String> tokens = new HashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 3 && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    double jaccard(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size();
    }
}
