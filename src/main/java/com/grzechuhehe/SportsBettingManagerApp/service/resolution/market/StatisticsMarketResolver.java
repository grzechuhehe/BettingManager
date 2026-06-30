package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StatisticsMarketResolver implements MarketResolver {

    private static final Set<MarketType> SUPPORTED = Set.of(
            MarketType.TEAM_TOTAL_SHOTS,
            MarketType.TOTAL_CARDS_OVER_UNDER,
            MarketType.PLAYER_TOTAL_SHOTS,
            MarketType.PLAYER_SHOTS,
            MarketType.PLAYER_SHOTS_ON_TARGET,
            MarketType.PLAYER_FOULS,
            MarketType.PLAYER_FOULS_COMMITTED_AGAINST,
            MarketType.CORNERS_HEAD_TO_HEAD,
            MarketType.CORNER_1X2
    );

    private static final Pattern PLAYER_SHOTS_PATTERN =
            Pattern.compile("(?i)(\\d+)\\+?\\s*(?:strza|shot)");

    private final ResolutionNameTranslator nameTranslator;

    @Override
    public boolean supports(Bet bet) {
        return bet.getMarketType() != null && SUPPORTED.contains(bet.getMarketType());
    }

    @Override
    public Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event) {
        if (!supports(bet) || event.getStatistics() == null || event.getStatistics().isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> stats = event.getStatistics();
        return switch (bet.getMarketType()) {
            case TEAM_TOTAL_SHOTS -> evaluateTeamOverUnder(
                    stats, event, bet, "shot", "shots", "totalshots", "totalshot");
            case TOTAL_CARDS_OVER_UNDER -> evaluateMatchOverUnder(stats, bet, "card", "cards", "yellowcard");
            case CORNERS_HEAD_TO_HEAD, CORNER_1X2 -> evaluateCornerWinner(stats, event, bet);
            case PLAYER_TOTAL_SHOTS, PLAYER_SHOTS -> evaluatePlayerThreshold(
                    stats, bet, "shot", "shots", "totalshots");
            case PLAYER_SHOTS_ON_TARGET -> evaluatePlayerThreshold(
                    stats, bet, "shotsontarget", "shotontarget", "sot");
            case PLAYER_FOULS -> evaluatePlayerThreshold(stats, bet, "foul", "foulscommitted");
            case PLAYER_FOULS_COMMITTED_AGAINST -> evaluatePlayerThreshold(
                    stats, bet, "foulsuffered", "foulsagainst", "foulsdrawn");
            default -> Optional.empty();
        };
    }

    private Optional<BetStatus> evaluateTeamOverUnder(
            Map<String, Object> stats,
            SofaScoreEventDto event,
            Bet bet,
            String... keyFragments) {
        String side = resolveTeamSide(event, bet.getSelection());
        if (side == null) {
            return Optional.empty();
        }
        Integer value = SofaScoreStatisticsReader.teamTotal(stats, side, keyFragments);
        return evaluateOverUnderLine(bet, value);
    }

    private Optional<BetStatus> evaluateMatchOverUnder(
            Map<String, Object> stats, Bet bet, String... keyFragments) {
        Integer value = SofaScoreStatisticsReader.matchTotal(stats, keyFragments);
        return evaluateOverUnderLine(bet, value);
    }

    private Optional<BetStatus> evaluateCornerWinner(
            Map<String, Object> stats, SofaScoreEventDto event, Bet bet) {
        Integer homeCorners = SofaScoreStatisticsReader.teamTotal(stats, "home", "corner");
        Integer awayCorners = SofaScoreStatisticsReader.teamTotal(stats, "away", "corner");
        if (homeCorners == null || awayCorners == null) {
            return Optional.empty();
        }
        String picked = mapTeamSelection(event, bet.getSelection());
        if (picked == null) {
            if (homeCorners > awayCorners) {
                return Optional.of(BetStatus.WON);
            }
            if (awayCorners > homeCorners) {
                return Optional.of(BetStatus.LOST);
            }
            return Optional.of(BetStatus.VOID);
        }
        boolean homePicked = "home".equals(picked);
        boolean won = homePicked ? homeCorners > awayCorners : awayCorners > homeCorners;
        if (homeCorners.equals(awayCorners)) {
            return Optional.of(BetStatus.VOID);
        }
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private Optional<BetStatus> evaluatePlayerThreshold(
            Map<String, Object> stats, Bet bet, String... keyFragments) {
        String player = extractPlayerName(bet.getSelection());
        if (player == null) {
            return Optional.empty();
        }
        Integer value = findPlayerStat(stats, player, keyFragments);
        if (value == null) {
            return Optional.empty();
        }
        Double line = resolveLine(bet);
        if (line == null) {
            Matcher m = PLAYER_SHOTS_PATTERN.matcher(bet.getSelection());
            if (m.find()) {
                line = Double.parseDouble(m.group(1)) - 0.5;
            }
        }
        if (line == null) {
            return Optional.empty();
        }
        boolean isOver = isOverSelection(bet.getSelection());
        if (!isOver && !isUnderSelection(bet.getSelection())) {
            isOver = true;
        }
        return evaluateOverUnder(value.doubleValue(), line, isOver, isUnderSelection(bet.getSelection()));
    }

    @SuppressWarnings("unchecked")
    private Integer findPlayerStat(Map<String, Object> stats, String player, String... keyFragments) {
        String normalizedPlayer = MarketResolutionUtils.normalize(player);
        return findPlayerStatRecursive(stats, normalizedPlayer, keyFragments);
    }

    @SuppressWarnings("unchecked")
    private Integer findPlayerStatRecursive(
            Map<String, Object> node, String normalizedPlayer, String... keyFragments) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String keyNorm = MarketResolutionUtils.normalize(entry.getKey());
            Object value = entry.getValue();
            if (keyNorm.contains(normalizedPlayer) || normalizedPlayer.contains(keyNorm)) {
                if (value instanceof Number number) {
                    return number.intValue();
                }
                if (value instanceof Map<?, ?> playerMap) {
                    Integer stat = SofaScoreStatisticsReader.teamTotal(
                            (Map<String, Object>) playerMap, "home", keyFragments);
                    if (stat == null) {
                        stat = findIntForKeys((Map<String, Object>) playerMap, keyFragments);
                    }
                    if (stat != null) {
                        return stat;
                    }
                }
            }
            if (value instanceof Map<?, ?> nested) {
                Integer found = findPlayerStatRecursive((Map<String, Object>) nested, normalizedPlayer, keyFragments);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Integer findIntForKeys(Map<String, Object> map, String... keyFragments) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            for (String fragment : keyFragments) {
                if (key.contains(fragment)) {
                    if (entry.getValue() instanceof Number n) {
                        return n.intValue();
                    }
                }
            }
        }
        return null;
    }

    private Optional<BetStatus> evaluateOverUnderLine(Bet bet, Integer actual) {
        if (actual == null) {
            return Optional.empty();
        }
        Double line = resolveLine(bet);
        if (line == null) {
            return Optional.empty();
        }
        boolean isOver = isOverSelection(bet.getSelection());
        boolean isUnder = isUnderSelection(bet.getSelection());
        if (isOver == isUnder) {
            isOver = !bet.getSelection().toLowerCase(Locale.ROOT).contains("under")
                    && !bet.getSelection().toLowerCase(Locale.ROOT).contains("poni");
        }
        return evaluateOverUnder(actual.doubleValue(), line, isOver, isUnder);
    }

    private Optional<BetStatus> evaluateOverUnder(
            double actual, double line, boolean isOver, boolean isUnder) {
        if (actual == line) {
            return Optional.of(BetStatus.VOID);
        }
        boolean over = actual > line;
        boolean won = (isOver && over) || (isUnder && !over);
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private Double resolveLine(Bet bet) {
        Double line = MarketResolutionUtils.parseNumber(bet.getLine());
        if (line == null) {
            line = MarketResolutionUtils.parseNumber(bet.getSelection());
        }
        return line;
    }

    private boolean isOverSelection(String selection) {
        String lower = selection.toLowerCase(Locale.ROOT);
        return lower.contains("over") || lower.contains("powyzej") || lower.contains("powyżej")
                || lower.matches(".*\\d+\\+.*");
    }

    private boolean isUnderSelection(String selection) {
        String lower = selection.toLowerCase(Locale.ROOT);
        return lower.contains("under") || lower.contains("ponizej") || lower.contains("poniżej");
    }

    private String extractPlayerName(String selection) {
        if (selection == null || selection.isBlank()) {
            return null;
        }
        String trimmed = selection;
        int overIdx = trimmed.toLowerCase(Locale.ROOT).indexOf(" over ");
        if (overIdx > 0) {
            return trimmed.substring(0, overIdx).trim();
        }
        Matcher shots = Pattern.compile("(?i)^(.+?)\\s+\\d+\\+").matcher(trimmed);
        if (shots.find()) {
            return shots.group(1).trim();
        }
        return trimmed.split("\\s+")[0];
    }

    private String resolveTeamSide(SofaScoreEventDto event, String selection) {
        String picked = mapTeamSelection(event, selection);
        return picked;
    }

    private String mapTeamSelection(SofaScoreEventDto event, String selection) {
        if (selection == null || selection.isBlank()) {
            return null;
        }
        String lower = selection.toLowerCase(Locale.ROOT);
        if (lower.startsWith("over") || lower.startsWith("under") || lower.startsWith("powy")
                || lower.startsWith("poni")) {
            return null;
        }
        String home = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getHomeTeam()));
        String away = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getAwayTeam()));
        String beforeOver = selection.split("(?i)\\s+(over|under|powy|poni)")[0];
        String sel = MarketResolutionUtils.normalize(nameTranslator.translateSegment(beforeOver.trim()));
        if (!sel.isEmpty() && !home.isEmpty() && (home.contains(sel) || sel.contains(home))) {
            return "home";
        }
        if (!sel.isEmpty() && !away.isEmpty() && (away.contains(sel) || sel.contains(away))) {
            return "away";
        }
        return null;
    }
}
