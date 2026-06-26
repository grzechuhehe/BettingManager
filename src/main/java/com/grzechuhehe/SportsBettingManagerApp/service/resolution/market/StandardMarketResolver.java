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
            MarketType.TOTALS_OVER_UNDER,
            MarketType.BOTH_TEAMS_TO_SCORE,
            MarketType.CORRECT_SCORE
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
            case MONEYLINE_1X2 -> evaluateMoneyline(event, home, away, selection, false);
            case MONEYLINE_12 -> evaluateMoneyline(event, home, away, selection, true);
            case TOTALS_OVER_UNDER -> evaluateTotals(bet, home, away, selection);
            case BOTH_TEAMS_TO_SCORE -> evaluateBtts(home, away, selection);
            case CORRECT_SCORE -> evaluateCorrectScore(bet, home, away, selection);
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
        String lastName = sel.contains(" ") ? sel.substring(0, sel.indexOf(' ')) : sel;
        if (!lastName.isEmpty() && away.contains(lastName)) {
            return "away";
        }
        if (!lastName.isEmpty() && home.contains(lastName)) {
            return "home";
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
        if (total == line) {
            return Optional.of(BetStatus.VOID);
        }
        boolean over = total > line;
        boolean won = (isOver && over) || (isUnder && !over);
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
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
