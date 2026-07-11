package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class BetStakeExtractionNormalizer {

    private static final BigDecimal PLN_PER_UNIT = new BigDecimal("10");
    private static final BigDecimal DEFAULT_STAKE = new BigDecimal("10");
    private static final BigDecimal MAX_REASONABLE_UNITS = new BigDecimal("50");
    private static final BigDecimal APPROX_TOLERANCE = new BigDecimal("0.02");

    private static final Set<String> POLISH_BOOKMAKERS = Set.of(
            "STS", "FORTUNA", "SUPERBET", "BETCLIC", "TOTALBET", "FORBET",
            "ETOTO", "LV BET", "LVBET", "PZBUK", "BETFAN");

    private final BigDecimal polishNetWinFactor;

    public BetStakeExtractionNormalizer(
            @Value("${bet.import.polish-net-win-factor:0.88}") BigDecimal polishNetWinFactor) {
        this.polishNetWinFactor = polishNetWinFactor;
    }

    public BetStakeNormalizationResult normalize(
            BigDecimal extractedStake,
            BigDecimal extractedUnits,
            String stakeCurrency,
            BigDecimal potentialWin,
            String bookmaker,
            BigDecimal odds) {

        String currency = resolveCurrency(stakeCurrency, bookmaker);
        BigDecimal stake = sanitizePositive(extractedStake);
        BigDecimal units = sanitizePositive(extractedUnits);
        BigDecimal win = sanitizePositive(potentialWin);

        if (stake != null && win != null && odds != null && odds.compareTo(BigDecimal.ONE) > 0) {
            if (isApproximatelyEqual(stake, win)) {
                stake = deriveStakeFromGrossWin(win, odds);
                log.warn("Stake matched potential win ({}); derived stake {} from odds {}", win, stake, odds);
            } else if (stake.compareTo(win) > 0) {
                stake = deriveStakeFromGrossWin(stake, odds);
                log.warn("Stake exceeded potential win; derived stake {} from gross win amount", stake);
            }
        }

        if (stake == null && win != null && odds != null && odds.compareTo(BigDecimal.ONE) > 0) {
            stake = deriveStakeFromNetWin(win, odds);
            log.info("Derived stake {} from net potential win {}", stake, win);
        }

        if (units != null && (units.compareTo(MAX_REASONABLE_UNITS) > 0
                || (stake != null && units.compareTo(stake) == 0))) {
            log.warn("Ignoring suspicious units value {}", units);
            units = null;
        }

        if (stake != null && stake.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal derivedUnits = stake.divide(PLN_PER_UNIT, 2, RoundingMode.HALF_UP);
            if (units == null) {
                units = derivedUnits;
            }
            return new BetStakeNormalizationResult(
                    scaleMoney(stake), scaleMoney(units), currency);
        }

        if (units != null && units.compareTo(BigDecimal.ZERO) > 0) {
            return new BetStakeNormalizationResult(
                    scaleMoney(units.multiply(PLN_PER_UNIT)),
                    scaleMoney(units),
                    "PLN");
        }

        return new BetStakeNormalizationResult(DEFAULT_STAKE, BigDecimal.ONE, "PLN");
    }

    private String resolveCurrency(String stakeCurrency, String bookmaker) {
        if (stakeCurrency != null && !stakeCurrency.isBlank()) {
            return stakeCurrency.trim().toUpperCase(Locale.ROOT);
        }
        if (isPolishBookmaker(bookmaker)) {
            return "PLN";
        }
        return "PLN";
    }

    private boolean isPolishBookmaker(String bookmaker) {
        if (bookmaker == null || bookmaker.isBlank()) {
            return false;
        }
        String normalized = bookmaker.trim().toUpperCase(Locale.ROOT);
        return POLISH_BOOKMAKERS.stream().anyMatch(normalized::contains);
    }

    private BigDecimal deriveStakeFromGrossWin(BigDecimal grossWin, BigDecimal odds) {
        return grossWin.divide(odds, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal deriveStakeFromNetWin(BigDecimal netWin, BigDecimal odds) {
        BigDecimal divisor = odds.multiply(polishNetWinFactor);
        return netWin.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }

    private boolean isApproximatelyEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        BigDecimal max = a.max(b);
        if (max.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        BigDecimal diff = a.subtract(b).abs();
        return diff.divide(max, 4, RoundingMode.HALF_UP).compareTo(APPROX_TOLERANCE) <= 0;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
