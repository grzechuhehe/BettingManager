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
    /** Kategoria płci — nie identyfikuje drużyny, tylko obniża Jaccarda przy query „Brazil Women”. */
    private static final Set<String> CATEGORY_TOKENS = Set.of("women", "womens", "men", "mens", "female", "male");

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

        boolean womensBet = isWomensBet(bet);
        MatchCandidate best = null;
        for (SofaScoreEventDto event : events) {
            if (!withinDateWindow(bet.getPlacedAt(), event.getStartTimestamp(), dateWindowDays)) {
                continue;
            }
            boolean womensEvent = isWomensEvent(event);
            boolean mensEvent = isMensEvent(event);
            if (womensBet && mensEvent) {
                continue;
            }
            if (!womensBet && womensEvent) {
                continue;
            }
            Set<String> eventTokens = new HashSet<>();
            eventTokens.addAll(tokenize(event.getHomeTeam()));
            eventTokens.addAll(tokenize(event.getAwayTeam()));
            if (eventTokens.isEmpty()) {
                continue;
            }
            double confidence = jaccard(withoutCategoryTokens(betTokens), withoutCategoryTokens(eventTokens));
            confidence = Math.max(confidence, queryTokenConfidence(bet, eventTokens));
            if (best == null || confidence > best.confidence()) {
                best = new MatchCandidate(event, confidence);
            }
        }
        return Optional.ofNullable(best);
    }

    boolean isWomensBet(Bet bet) {
        if (bet.getEventName() == null) {
            return false;
        }
        String lower = bet.getEventName().toLowerCase(Locale.ROOT);
        return lower.contains("(k)") || lower.contains("(w)") || lower.contains("women");
    }

    boolean isWomensEvent(SofaScoreEventDto event) {
        return hasGenderWord(combinedEventText(event), "women", "womens", "female");
    }

    boolean isMensEvent(SofaScoreEventDto event) {
        return hasGenderWord(combinedEventText(event), "men", "mens", "male");
    }

    private boolean hasGenderWord(String text, String... words) {
        String padded = genderMarker(text);
        for (String word : words) {
            if (padded.contains(" " + word + " ")) {
                return true;
            }
        }
        return false;
    }

    private String combinedEventText(SofaScoreEventDto event) {
        StringBuilder sb = new StringBuilder();
        if (event.getTournament() != null) {
            sb.append(event.getTournament()).append(' ');
        }
        if (event.getName() != null) {
            sb.append(event.getName()).append(' ');
        }
        if (event.getHomeTeam() != null) {
            sb.append(event.getHomeTeam()).append(' ');
        }
        if (event.getAwayTeam() != null) {
            sb.append(event.getAwayTeam());
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** „women” / „men” jako osobne słowa — nie traktujemy „Germany” jako „men”. */
    private String genderMarker(String text) {
        return " " + text.replaceAll("[^a-z]", " ") + " ";
    }

    private Set<String> withoutCategoryTokens(Set<String> tokens) {
        Set<String> filtered = new HashSet<>(tokens);
        filtered.removeAll(CATEGORY_TOKENS);
        return filtered;
    }

    /**
     * Osobny score z angielskiej frazy Apify (np. „Croatia Slovenia”) — bez polskich tokenów
     * „Chorwacja/Słowenia”, które zaniżają Jaccarda do ~0.5 mimo poprawnego meczu.
     */
    private double queryTokenConfidence(Bet bet, Set<String> eventTokens) {
        return nameTranslator.resolveQueryForApify(bet.getEventName())
                .map(q -> jaccard(withoutCategoryTokens(tokenize(q)), withoutCategoryTokens(eventTokens)))
                .orElse(0.0);
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
