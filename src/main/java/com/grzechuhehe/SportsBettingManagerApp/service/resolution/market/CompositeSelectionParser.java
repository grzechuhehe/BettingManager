package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CompositeSelectionParser {

    private static final Pattern HANDICAP_FRAGMENT =
            Pattern.compile("(?i)handicap\\s+(\\d+)\\s*:\\s*(\\d+)\\s*:\\s*(.+)");
    private static final Pattern HANDICAP_NUMERIC_FRAGMENT =
            Pattern.compile("(?i)handicap\\s+([+-]?\\d+(?:[.,]\\d+)?)\\s*:\\s*(.+)");

    public List<AtomicCondition> parse(String selection) {
        if (selection == null || selection.isBlank()) {
            return List.of();
        }
        List<AtomicCondition> conditions = new ArrayList<>();
        for (String fragment : splitFragments(stripPrefix(selection))) {
            parseFragment(fragment).ifPresent(conditions::add);
        }
        return conditions;
    }

    /**
     * Zwraca komplet warunków tylko gdy KAŻDY fragment jest rozpoznany.
     * Pusty Optional = nie potrafimy bezpiecznie rozliczyć → ręczne rozliczenie.
     */
    public java.util.Optional<List<AtomicCondition>> parseComplete(String selection) {
        if (selection == null || selection.isBlank()) {
            return java.util.Optional.empty();
        }
        List<String> fragments = splitFragments(stripPrefix(selection));
        if (fragments.isEmpty()) {
            return java.util.Optional.empty();
        }
        List<AtomicCondition> conditions = new ArrayList<>();
        for (String fragment : fragments) {
            java.util.Optional<AtomicCondition> parsed = parseFragment(fragment);
            if (parsed.isEmpty()) {
                return java.util.Optional.empty();
            }
            conditions.add(parsed.get());
        }
        return java.util.Optional.of(conditions);
    }

    private String stripPrefix(String selection) {
        String text = selection.trim();
        if (text.toLowerCase(Locale.ROOT).startsWith("betbuilder:")) {
            return text.substring("betbuilder:".length()).trim();
        }
        if (text.toLowerCase(Locale.ROOT).startsWith("bet builder:")) {
            return text.substring("bet builder:".length()).trim();
        }
        return text;
    }

    private java.util.Optional<AtomicCondition> parseFragment(String fragment) {
        String lower = fragment.toLowerCase(Locale.ROOT);
        if (lower.contains("suma") || lower.contains("over") || lower.contains("under")
                || lower.contains("powyzej") || lower.contains("powyżej")
                || lower.contains("ponizej") || lower.contains("poniżej")) {
            Double line = MarketResolutionUtils.parseNumber(fragment);
            if (line == null) {
                return java.util.Optional.empty();
            }
            boolean over = lower.contains("over") || lower.contains("powyzej") || lower.contains("powyżej");
            String sel = over ? "over " + line : "under " + line;
            return java.util.Optional.of(new AtomicCondition(
                    MarketType.TOTALS_OVER_UNDER, sel, String.valueOf(line)));
        }

        Matcher handicap = HANDICAP_FRAGMENT.matcher(fragment);
        if (handicap.find()) {
            int homeStart = Integer.parseInt(handicap.group(1));
            int awayStart = Integer.parseInt(handicap.group(2));
            String teamPart = handicap.group(3).replaceAll("(?i)\\(\\d+\\s*:\\s*\\d+\\)", "").trim();
            String line = homeStart + ":" + awayStart;
            return java.util.Optional.of(new AtomicCondition(
                    MarketType.HANDICAP, teamPart, line));
        }

        Matcher numericHandicap = HANDICAP_NUMERIC_FRAGMENT.matcher(fragment);
        if (numericHandicap.find()) {
            String rawLine = numericHandicap.group(1).replace(',', '.');
            String teamPart = numericHandicap.group(2)
                    .replaceAll("(?i)\\([+-]?\\d+(?:[.,]\\d+)?\\)", "").trim();
            return java.util.Optional.of(new AtomicCondition(
                    MarketType.HANDICAP, teamPart, rawLine));
        }

        return java.util.Optional.empty();
    }

    static List<String> splitFragments(String text) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '(') {
                depth++;
            }
            if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                if (!current.isEmpty()) {
                    parts.add(current.toString().trim());
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }
}
