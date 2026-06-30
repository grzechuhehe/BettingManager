package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

@Component
@RequiredArgsConstructor
public class StandardMarketResolver implements MarketResolver {

    private static final Set<MarketType> SUPPORTED = Set.of(
            MarketType.MONEYLINE_1X2,
            MarketType.MONEYLINE_12,
            MarketType.MATCH_ODDS,
            MarketType.TOTALS_OVER_UNDER,
            MarketType.OVER_UNDER,
            MarketType.TEAM_TOTAL_GOALS,
            MarketType.TEAM_TOTAL_GOALS_OVER_UNDER,
            MarketType.BOTH_TEAMS_TO_SCORE,
            MarketType.CORRECT_SCORE,
            MarketType.DOUBLE_CHANCE
    );

    private final ResolutionNameTranslator nameTranslator;

    @Override
    public boolean supports(Bet bet) {
        return bet.getMarketType() != null && SUPPORTED.contains(bet.getMarketType());
    }

    @Override
    public Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event) {
        if (!supports(bet)) {
            return Optional.empty();
        }
        int home = event.getHomeScore();
        int away = event.getAwayScore();
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT).trim();

        return switch (bet.getMarketType()) {
            case MONEYLINE_1X2, MATCH_ODDS -> evaluateMoneyline(event, home, away, selection, false);
            case MONEYLINE_12 -> evaluateMoneyline(event, home, away, selection, true);
            case TOTALS_OVER_UNDER, OVER_UNDER -> evaluateTotals(bet, home, away, selection);
            case TEAM_TOTAL_GOALS, TEAM_TOTAL_GOALS_OVER_UNDER ->
                    evaluateTeamGoals(bet, event, home, away, selection);
            case BOTH_TEAMS_TO_SCORE -> evaluateBtts(home, away, selection);
            case CORRECT_SCORE -> evaluateCorrectScore(bet, home, away, selection);
            case DOUBLE_CHANCE -> evaluateDoubleChance(event, home, away, selection);
            default -> Optional.empty();
        };
    }

    private Optional<BetStatus> evaluateMoneyline(
            SofaScoreEventDto event, int home, int away, String selection, boolean drawVoids) {
        String winner = resolveWinner(event, home, away);
        if (drawVoids && winner.equals("draw")) {
            return Optional.of(BetStatus.VOID);
        }
        String picked = mapMoneylineSelection(event, selection);
        if (picked == null) {
            return Optional.empty();
        }
        return Optional.of(picked.equals(winner) ? BetStatus.WON : BetStatus.LOST);
    }

    private String resolveWinner(SofaScoreEventDto event, int home, int away) {
        if (event.getWinnerCode() != null) {
            return switch (event.getWinnerCode()) {
                case 1 -> "home";
                case 2 -> "away";
                default -> "draw";
            };
        }
        return home > away ? "home" : (away > home ? "away" : "draw");
    }

    private String mapMoneylineSelection(SofaScoreEventDto event, String selection) {
        if (selection.isEmpty()) {
            return null;
        }
        if (selection.equals("1") || selection.equals("home")) {
            return "home";
        }
        if (selection.equals("2") || selection.equals("away")) {
            return "away";
        }
        if (selection.equals("x") || selection.equals("draw") || selection.equals("remis")) {
            return "draw";
        }
        String home = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getHomeTeam()));
        String away = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getAwayTeam()));
        String sel = MarketResolutionUtils.normalize(nameTranslator.translateSegment(selection));
        if (!sel.isEmpty() && !home.isEmpty() && (home.contains(sel) || sel.contains(home))) {
            return "home";
        }
        if (!sel.isEmpty() && !away.isEmpty() && (away.contains(sel) || sel.contains(away))) {
            return "away";
        }
        return null;
    }

    private Optional<BetStatus> evaluateTotals(Bet bet, int home, int away, String selection) {
        Double line = MarketResolutionUtils.parseNumber(bet.getLine());
        if (line == null) {
            line = MarketResolutionUtils.parseNumber(selection);
        }
        if (line == null) {
            return Optional.empty();
        }
        boolean isOver = selection.contains("over") || selection.contains("powyzej") || selection.contains("powyżej");
        boolean isUnder = selection.contains("under") || selection.contains("ponizej") || selection.contains("poniżej");
        if (isOver == isUnder) {
            return Optional.empty();
        }
        int total = home + away;
        return MarketResolutionUtils.resolveOverUnder(total, line, isOver, isUnder);
    }

    private Optional<BetStatus> evaluateBtts(int home, int away, String selection) {
        boolean bothScored = home > 0 && away > 0;
        boolean pickYes = selection.contains("yes") || selection.contains("tak") || selection.contains("gg");
        boolean pickNo = selection.contains("nie") || selection.equals("no") || selection.contains("ng");
        if (pickYes == pickNo) {
            return Optional.empty();
        }
        boolean won = (pickYes && bothScored) || (pickNo && !bothScored);
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private Optional<BetStatus> evaluateTeamGoals(
            Bet bet, SofaScoreEventDto event, int home, int away, String selection) {
        String side = resolveTeamSideForGoals(event, selection);
        if (side == null) {
            return Optional.empty();
        }
        int teamGoals = "home".equals(side) ? home : away;
        Double line = MarketResolutionUtils.parseNumber(bet.getLine());
        if (line == null) {
            line = MarketResolutionUtils.parseNumber(selection);
        }
        if (line == null) {
            return Optional.empty();
        }
        boolean isOver = selection.contains("over") || selection.contains("powyzej")
                || selection.contains("powyżej");
        boolean isUnder = selection.contains("under") || selection.contains("ponizej")
                || selection.contains("poniżej");
        if (isOver == isUnder) {
            return Optional.empty();
        }
        return MarketResolutionUtils.resolveOverUnder(teamGoals, line, isOver, isUnder);
    }

    private String resolveTeamSideForGoals(SofaScoreEventDto event, String selection) {
        if (selection == null || selection.isBlank()) {
            return null;
        }
        String home = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getHomeTeam()));
        String away = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getAwayTeam()));
        String beforeMetric = selection.split("(?i)\\s+(over|under|powy|poni|goals?)")[0];
        String sel = MarketResolutionUtils.normalize(nameTranslator.translateSegment(beforeMetric.trim()));
        if (!sel.isEmpty() && !home.isEmpty() && (home.contains(sel) || sel.contains(home))) {
            return "home";
        }
        if (!sel.isEmpty() && !away.isEmpty() && (away.contains(sel) || sel.contains(away))) {
            return "away";
        }
        return mapMoneylineSelection(event, selection.toLowerCase(Locale.ROOT).trim());
    }

    private Optional<BetStatus> evaluateDoubleChance(
            SofaScoreEventDto event, int home, int away, String selection) {
        String winner = resolveWinner(event, home, away);
        DoubleChancePick pick = parseDoubleChance(event, selection);
        if (pick == null) {
            return Optional.empty();
        }
        boolean won = switch (pick) {
            case HOME_OR_DRAW -> winner.equals("home") || winner.equals("draw");
            case AWAY_OR_DRAW -> winner.equals("away") || winner.equals("draw");
            case HOME_OR_AWAY -> winner.equals("home") || winner.equals("away");
        };
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private enum DoubleChancePick { HOME_OR_DRAW, AWAY_OR_DRAW, HOME_OR_AWAY }

    private DoubleChancePick parseDoubleChance(SofaScoreEventDto event, String selection) {
        String lower = selection.toLowerCase(Locale.ROOT);
        if (lower.equals("1x") || lower.contains("1x")) {
            return DoubleChancePick.HOME_OR_DRAW;
        }
        if (lower.equals("x2") || lower.contains("x2")) {
            return DoubleChancePick.AWAY_OR_DRAW;
        }
        if (lower.equals("12") || lower.contains("12")) {
            return DoubleChancePick.HOME_OR_AWAY;
        }
        String home = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getHomeTeam()));
        String away = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getAwayTeam()));
        boolean hasDraw = lower.contains("remis") || lower.contains("draw");
        boolean hasHomeRef = !home.isEmpty() && lower.contains(home);
        boolean hasAwayRef = !away.isEmpty() && lower.contains(away);
        if (hasDraw && hasHomeRef && !hasAwayRef) {
            return DoubleChancePick.HOME_OR_DRAW;
        }
        if (hasDraw && hasAwayRef && !hasHomeRef) {
            return DoubleChancePick.AWAY_OR_DRAW;
        }
        if (lower.contains("lub remis") || lower.contains("or draw")) {
            String teamPart = lower.split("lub remis|or draw")[0].trim();
            String teamNorm = MarketResolutionUtils.normalize(nameTranslator.translateSegment(teamPart));
            if (!teamNorm.isEmpty() && home.contains(teamNorm)) {
                return DoubleChancePick.HOME_OR_DRAW;
            }
            if (!teamNorm.isEmpty() && away.contains(teamNorm)) {
                return DoubleChancePick.AWAY_OR_DRAW;
            }
        }
        if (lower.contains("remis lub") || lower.contains("draw or")) {
            String teamPart = lower.split("remis lub|draw or")[1].trim();
            String teamNorm = MarketResolutionUtils.normalize(nameTranslator.translateSegment(teamPart));
            if (!teamNorm.isEmpty() && away.contains(teamNorm)) {
                return DoubleChancePick.AWAY_OR_DRAW;
            }
            if (!teamNorm.isEmpty() && home.contains(teamNorm)) {
                return DoubleChancePick.HOME_OR_DRAW;
            }
        }
        return null;
    }

    private Optional<BetStatus> evaluateCorrectScore(Bet bet, int home, int away, String selection) {
        String source = (bet.getLine() != null && MarketResolutionUtils.SCORE.matcher(bet.getLine()).find())
                ? bet.getLine()
                : selection;
        Matcher m = MarketResolutionUtils.SCORE.matcher(source);
        if (!m.find()) {
            return Optional.empty();
        }
        int pickedHome = Integer.parseInt(m.group(1));
        int pickedAway = Integer.parseInt(m.group(2));
        boolean won = pickedHome == home && pickedAway == away;
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }
}
