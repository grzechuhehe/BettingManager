package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class TennisNameNormalizer {

    private static final Pattern COMMA_NAME = Pattern.compile("^\\s*([^,]+)\\s*,\\s*(.+)$");
    private static final Pattern INITIAL_SUFFIX = Pattern.compile("^(.+?)\\s+[A-Z]\\.?$");

    public Set<String> extraTokens(String side) {
        Set<String> tokens = new HashSet<>();
        if (side == null || side.isBlank()) {
            return tokens;
        }
        String cleaned = side.replaceAll("(?i)\\s*-\\s*winner$", "").trim();
        var comma = COMMA_NAME.matcher(cleaned);
        if (comma.matches()) {
            add(tokens, comma.group(1));
            add(tokens, comma.group(2));
            return tokens;
        }
        var initial = INITIAL_SUFFIX.matcher(cleaned);
        if (initial.matches()) {
            add(tokens, initial.group(1));
        }
        add(tokens, cleaned);
        return tokens;
    }

    private static void add(Set<String> tokens, String raw) {
        String n = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ł", "l")
                .replaceAll("[^a-z0-9]", "");
        if (n.length() >= 3) {
            tokens.add(n);
        }
    }
}
