package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BetOutcomeEvaluator {

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern SCORE = Pattern.compile("(\\d+)\\s*[:\\-]\\s*(\\d+)");

    public Optional<BetStatus> evaluate(Bet bet, SofaScoreEventDto event) {
        if (event == null || bet.getMarketType() == null) {
            return Optional.empty();
        }
        String status = event.getStatusType() == null ? "" : event.getStatusType().toLowerCase(Locale.ROOT);
        if (status.equals("canceled") || status.equals("cancelled") || status.equals("postponed")) {
            return Optional.of(BetStatus.VOID);
        }
        if (!status.equals("finished") || event.getHomeScore() == null || event.getAwayScore() == null) {
            return Optional.empty();
        }
        int home = event.getHomeScore();
        int away = event.getAwayScore();
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT).trim();

        return switch (bet.getMarketType()) {
            case MONEYLINE_1X2, MONEYLINE_12 -> evaluateMoneyline(event, home, away, selection);
            case TOTALS_OVER_UNDER -> evaluateTotals(bet, home, away, selection);
            case BOTH_TEAMS_TO_SCORE -> evaluateBtts(home, away, selection);
            case CORRECT_SCORE -> evaluateCorrectScore(bet, home, away, selection);
            default -> Optional.empty();
        };
    }

    private Optional<BetStatus> evaluateMoneyline(SofaScoreEventDto event, int home, int away, String selection) {
        String winner = home > away ? "home" : (away > home ? "away" : "draw");
        String picked = mapMoneylineSelection(event, selection);
        if (picked == null) {
            return Optional.empty();
        }
        return Optional.of(picked.equals(winner) ? BetStatus.WON : BetStatus.LOST);
    }

    private String mapMoneylineSelection(SofaScoreEventDto event, String selection) {
        if (selection.isEmpty()) {
            return null;
        }
        if (selection.equals("1") || selection.equals("home")) return "home";
        if (selection.equals("2") || selection.equals("away")) return "away";
        if (selection.equals("x") || selection.equals("draw") || selection.equals("remis")) return "draw";
        String home = normalize(event.getHomeTeam());
        String away = normalize(event.getAwayTeam());
        String sel = normalize(selection);
        if (!sel.isEmpty() && !home.isEmpty() && (home.contains(sel) || sel.contains(home))) return "home";
        if (!sel.isEmpty() && !away.isEmpty() && (away.contains(sel) || sel.contains(away))) return "away";
        return null;
    }

    private Optional<BetStatus> evaluateTotals(Bet bet, int home, int away, String selection) {
        Double line = parseNumber(bet.getLine());
        if (line == null) {
            line = parseNumber(selection);
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
        String source = (bet.getLine() != null && SCORE.matcher(bet.getLine()).find()) ? bet.getLine() : selection;
        Matcher m = SCORE.matcher(source);
        if (!m.find()) {
            return Optional.empty();
        }
        int pickedHome = Integer.parseInt(m.group(1));
        int pickedAway = Integer.parseInt(m.group(2));
        boolean won = pickedHome == home && pickedAway == away;
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private Double parseNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = NUMBER.matcher(text.replace(',', '.'));
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
