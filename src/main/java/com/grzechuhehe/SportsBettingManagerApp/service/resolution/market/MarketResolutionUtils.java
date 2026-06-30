package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarketResolutionUtils {

    static final Pattern NUMBER = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)");
    static final Pattern SCORE = Pattern.compile("(\\d+)\\s*[:\\-]\\s*(\\d+)");

    private MarketResolutionUtils() {}

    /** Rozstrzyga zakład over/under: równość = VOID (push), inaczej WON/LOST wg strony. */
    static Optional<BetStatus> resolveOverUnder(double actual, double line, boolean isOver, boolean isUnder) {
        if (actual == line) {
            return Optional.of(BetStatus.VOID);
        }
        boolean over = actual > line;
        boolean won = (isOver && over) || (isUnder && !over);
        return Optional.of(won ? BetStatus.WON : BetStatus.LOST);
    }

    static Double parseNumber(String text) {
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

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ł", "l")
                .replaceAll("[^a-z0-9]", "");
    }

    static boolean isTennis(com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto event,
                            com.grzechuhehe.SportsBettingManagerApp.model.Bet bet) {
        if (event != null && event.getSport() != null
                && event.getSport().toLowerCase(Locale.ROOT).contains("tennis")) {
            return true;
        }
        if (bet != null && bet.getSport() != null) {
            String s = bet.getSport().toLowerCase(Locale.ROOT);
            return s.contains("tennis") || s.contains("tenis");
        }
        return false;
    }

    /** Wynik wygląda na liczbę setów (np. 2:0), nie gemów w secie (np. 4:6). */
    static boolean looksLikeSetScore(int home, int away) {
        return home <= 3 && away <= 3 && (home + away) <= 5;
    }
}
