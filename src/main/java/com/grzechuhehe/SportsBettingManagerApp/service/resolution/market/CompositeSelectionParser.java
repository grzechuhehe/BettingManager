package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
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

    private final ResolutionNameTranslator nameTranslator;

    public CompositeSelectionParser(ResolutionNameTranslator nameTranslator) {
        this.nameTranslator = nameTranslator;
    }

    public List<AtomicCondition> parse(String selection) {
        if (selection == null || selection.isBlank()) {
            return List.of();
        }
        String text = selection.trim();
        if (text.toLowerCase(Locale.ROOT).startsWith("betbuilder:")) {
            text = text.substring("betbuilder:".length()).trim();
        } else if (text.toLowerCase(Locale.ROOT).startsWith("bet builder:")) {
            text = text.substring("bet builder:".length()).trim();
        }

        List<AtomicCondition> conditions = new ArrayList<>();
        for (String fragment : splitFragments(text)) {
            parseFragment(fragment).ifPresent(conditions::add);
        }
        return conditions;
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
                    MarketType.TOTALS_OVER_UNDER, sel, String.valueOf(line), null));
        }

        Matcher handicap = HANDICAP_FRAGMENT.matcher(fragment);
        if (handicap.find()) {
            int homeStart = Integer.parseInt(handicap.group(1));
            int awayStart = Integer.parseInt(handicap.group(2));
            String teamPart = handicap.group(3).replaceAll("(?i)\\(\\d+\\s*:\\s*\\d+\\)", "").trim();
            String side = inferSide(teamPart);
            String line = homeStart + ":" + awayStart;
            return java.util.Optional.of(new AtomicCondition(
                    MarketType.HANDICAP, teamPart, line, side));
        }
        return java.util.Optional.empty();
    }

    private String inferSide(String teamPart) {
        String normalized = MarketResolutionUtils.normalize(nameTranslator.translateSegment(teamPart));
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
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
