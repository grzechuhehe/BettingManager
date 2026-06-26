package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class HandicapMarketResolver implements MarketResolver {

    private static final Pattern HANDICAP_IN_SELECTION =
            Pattern.compile("\\(([+-]?\\d+(?:[.,]\\d+)?)\\)");

    private final ResolutionNameTranslator nameTranslator;

    @Override
    public boolean supports(Bet bet) {
        if (bet.getMarketType() != null
                && bet.getMarketType() != MarketType.HANDICAP
                && bet.getMarketType() != MarketType.ASIAN_HANDICAP) {
            return false;
        }
        if (parseVirtualStartLine(bet.getLine()).isPresent()) {
            return bet.getSelection() != null && !bet.getSelection().isBlank();
        }
        if (bet.getSelection() != null && HANDICAP_IN_SELECTION.matcher(bet.getSelection()).find()) {
            return extractNumericLine(bet).isPresent();
        }
        return bet.getMarketType() == MarketType.HANDICAP || bet.getMarketType() == MarketType.ASIAN_HANDICAP;
    }

    @Override
    public Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event) {
        int home = event.getHomeScore();
        int away = event.getAwayScore();

        Optional<String> virtualLine = parseVirtualStartLine(bet.getLine());
        if (virtualLine.isPresent()) {
            return resolveVirtualStart(bet, event, home, away, virtualLine.get());
        }

        Optional<Double> lineOpt = extractNumericLine(bet);
        if (lineOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> sideOpt = inferSide(bet, event);
        if (sideOpt.isEmpty()) {
            return Optional.empty();
        }
        String side = sideOpt.get();
        double line = lineOpt.get();

        double adjustedHome = home;
        double adjustedAway = away;
        if ("home".equals(side)) {
            adjustedHome = home + line;
        } else {
            adjustedAway = away + line;
        }

        return compareHandicap(bet.getMarketType(), adjustedHome, adjustedAway, side);
    }

    Optional<Double> extractNumericLine(Bet bet) {
        if (bet.getLine() != null && parseVirtualStartLine(bet.getLine()).isEmpty()) {
            Double fromLine = MarketResolutionUtils.parseNumber(bet.getLine());
            if (fromLine != null) {
                return Optional.of(fromLine);
            }
        }
        if (bet.getSelection() == null) {
            return Optional.empty();
        }
        Matcher m = HANDICAP_IN_SELECTION.matcher(bet.getSelection());
        if (m.find()) {
            return Optional.ofNullable(MarketResolutionUtils.parseNumber(m.group(1)));
        }
        return Optional.empty();
    }

    private Optional<String> parseVirtualStartLine(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher m = MarketResolutionUtils.SCORE.matcher(line.trim());
        if (m.matches()) {
            return Optional.of(line.trim());
        }
        return Optional.empty();
    }

    private Optional<BetStatus> resolveVirtualStart(
            Bet bet, SofaScoreEventDto event, int home, int away, String virtualLine) {
        Matcher m = MarketResolutionUtils.SCORE.matcher(virtualLine);
        if (!m.find()) {
            return Optional.empty();
        }
        int homeStart = Integer.parseInt(m.group(1));
        int awayStart = Integer.parseInt(m.group(2));
        double effectiveHome = home + homeStart;
        double effectiveAway = away + awayStart;

        Optional<String> sideOpt = inferSide(bet, event);
        if (sideOpt.isEmpty()) {
            return Optional.empty();
        }
        return compareHandicap(bet.getMarketType(), effectiveHome, effectiveAway, sideOpt.get());
    }

    private Optional<BetStatus> compareHandicap(
            MarketType marketType, double effectiveHome, double effectiveAway, String side) {
        if (effectiveHome == effectiveAway) {
            if (marketType == MarketType.ASIAN_HANDICAP) {
                return Optional.of(BetStatus.VOID);
            }
            return Optional.of(BetStatus.LOST);
        }
        String winner = effectiveHome > effectiveAway ? "home" : "away";
        boolean won = winner.equals(side);
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    private Optional<String> inferSide(Bet bet, SofaScoreEventDto event) {
        if (bet.getLine() != null) {
            Optional<String> virtual = parseVirtualStartLine(bet.getLine());
            if (virtual.isPresent() && bet.getSelection() != null && event != null) {
                return mapSideFromSelection(event, bet.getSelection());
            }
        }
        String selection = bet.getSelection() == null ? "" : bet.getSelection();
        Matcher m = HANDICAP_IN_SELECTION.matcher(selection);
        String namePart = m.find() ? selection.substring(0, m.start()).trim() : selection.trim();
        if (namePart.isEmpty() && event == null) {
            return Optional.empty();
        }
        if (event == null) {
            return Optional.of("away");
        }
        return mapSideFromSelection(event, namePart);
    }

    private Optional<String> mapSideFromSelection(SofaScoreEventDto event, String selection) {
        String sel = MarketResolutionUtils.normalize(nameTranslator.translateSegment(selection));
        if (sel.isEmpty()) {
            return Optional.empty();
        }
        String home = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getHomeTeam()));
        String away = MarketResolutionUtils.normalize(nameTranslator.translateSegment(event.getAwayTeam()));
        if (!home.isEmpty() && (home.contains(sel) || sel.contains(home))) {
            return Optional.of("home");
        }
        if (!away.isEmpty() && (away.contains(sel) || sel.contains(away))) {
            return Optional.of("away");
        }
        String lastName = sel.split(" ")[0];
        if (!lastName.isEmpty() && away.contains(lastName)) {
            return Optional.of("away");
        }
        if (!lastName.isEmpty() && home.contains(lastName)) {
            return Optional.of("home");
        }
        return Optional.empty();
    }
}
