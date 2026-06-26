package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarketResolutionUtils {

    static final Pattern NUMBER = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)");
    static final Pattern SCORE = Pattern.compile("(\\d+)\\s*[:\\-]\\s*(\\d+)");

    private MarketResolutionUtils() {}

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
}
