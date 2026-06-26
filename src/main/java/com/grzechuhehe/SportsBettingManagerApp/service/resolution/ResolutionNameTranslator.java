package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mapuje polskie skróty/nazwy z bukmachera na frazy do SofaScore (EN).
 */
@Component
public class ResolutionNameTranslator {

    private static final Pattern VS_SPLIT = Pattern.compile("\\s+vs\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH_SPLIT = Pattern.compile("\\s+-\\s+");
    private static final Pattern SIDE_SPLIT = Pattern.compile("\\s*(?:vs|v\\.|–|—|-|:)\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern REJECTED_EVENT = Pattern.compile(
            "[;&|]|\\&|\\b(kupon|ako|parlay|akumulacyjny|bet builder|multi.?event|dokladna klasyfikacja)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Set<String> REJECTED_SIDE_WORDS = Set.of(
            "kupon", "ako", "parlay", "akumulacyjny", "mistrzostwa", "grupa", "awansuje", "klasyfikacja", "koncowa"
    );

    private static final Set<String> REJECTED_NORMALIZED_FRAGMENTS = Set.of(
            "kupon", "akumulacyjny", "kl koncowa", "awansuje", "dokladna klasyfikacja"
    );

    private static final Map<String, String> SEGMENT_ALIASES = buildAliases();

    public record TwoSides(String home, String away) {}

    public boolean hasTwoSides(String eventName) {
        return parseTwoTeamSides(eventName).isPresent();
    }

    public boolean isSearchableEventName(String eventName) {
        return resolveQueryForApify(eventName).isPresent();
    }

    public String toSearchQuery(String eventName) {
        return resolveQueryForApify(eventName).orElse("");
    }

    /**
     * Fraza do Apify: tłumaczenie PL→EN gdy możliwe, inaczej „Home Away” (Apify i tak fuzzy-matchuje).
     * Pomija tylko nieznane mikro-skroty typu WKS (≤3 znaki).
     */
    public Optional<String> resolveQueryForApify(String eventName) {
        return parseTwoTeamSides(eventName).flatMap(sides -> {
            if (isUnknownAbbrev(sides.home()) || isUnknownAbbrev(sides.away())) {
                return Optional.empty();
            }
            String home = apifySearchSegment(sides.home());
            String away = apifySearchSegment(sides.away());
            if (home.isBlank() || away.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(home + " " + away);
        });
    }

    /** @deprecated użyj {@link #resolveQueryForApify} */
    public Optional<String> buildSearchQuery(String eventName) {
        return resolveQueryForApify(eventName);
    }

    public Optional<TwoSides> parseTwoTeamSides(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = eventName.trim();
        if (REJECTED_EVENT.matcher(trimmed).find() || containsRejectedNormalized(trimmed)) {
            return Optional.empty();
        }

        String[] parts;
        if (VS_SPLIT.matcher(trimmed).find()) {
            parts = VS_SPLIT.split(trimmed, 2);
        } else if (DASH_SPLIT.matcher(trimmed).find()) {
            parts = DASH_SPLIT.split(trimmed, 2);
        } else {
            return Optional.empty();
        }
        if (parts.length != 2) {
            return Optional.empty();
        }

        String home = parts[0].trim();
        String away = parts[1].trim();
        if (!isValidSide(home) || !isValidSide(away)) {
            return Optional.empty();
        }
        return Optional.of(new TwoSides(home, away));
    }

    private boolean isValidSide(String side) {
        if (side.length() < 2 || side.length() > 45) {
            return false;
        }
        if (side.matches("^\\d.*")) {
            return false;
        }
        String normalized = normalize(side);
        for (String word : normalized.split("\\s+")) {
            if (REJECTED_SIDE_WORDS.contains(word)) {
                return false;
            }
        }
        return normalized.split("\\s+").length <= 6;
    }

    private boolean isUnknownAbbrev(String side) {
        String key = normalize(side);
        return key.length() <= 3 && !SEGMENT_ALIASES.containsKey(key);
    }

    private boolean containsRejectedNormalized(String eventName) {
        String normalized = normalize(eventName);
        for (String fragment : REJECTED_NORMALIZED_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tokeny do dopasowania: oryginał + angielskie aliasy obu stron.
     */
    public Set<String> matchingTokens(String eventName, Set<String> baseTokens) {
        Set<String> tokens = new HashSet<>(baseTokens);
        if (eventName == null || eventName.isBlank()) {
            return tokens;
        }
        parseTwoTeamSides(eventName).ifPresent(sides -> {
            addTokenized(tokens, translateSegment(sides.home()));
            addTokenized(tokens, translateSegment(sides.away()));
            resolveQueryForApify(eventName).ifPresent(q -> addTokenized(tokens, q));
        });
        String[] legacyParts = SIDE_SPLIT.split(eventName.trim());
        for (String part : legacyParts) {
            String translated = translateSegment(part.trim());
            if (!translated.isBlank()) {
                addTokenized(tokens, translated);
            }
        }
        return tokens;
    }

    private static void addTokenized(Set<String> tokens, String text) {
        String normalized = normalize(text);
        for (String word : normalized.split("\\s+")) {
            if (word.length() >= 3) {
                tokens.add(word);
            }
        }
    }

    public String translateSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "";
        }
        String key = normalize(segment);
        if (SEGMENT_ALIASES.containsKey(key)) {
            return SEGMENT_ALIASES.get(key);
        }
        for (Map.Entry<String, String> entry : SEGMENT_ALIASES.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }
        return segment.trim();
    }

    /** Reprezentacje (K) — SofaScore w search wymaga sufiksu Women. */
    private String apifySearchSegment(String segment) {
        String translated = translateSegment(segment);
        if (segment != null && segment.toLowerCase(Locale.ROOT).contains("(k)")) {
            if (!translated.toLowerCase(Locale.ROOT).contains("women")) {
                translated = translated + " Women";
            }
        }
        return translated;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ł", "l") // NFD nie rozkłada ł — bez tego "Korea Płd." → "korea pd"
                .replaceAll("\\(k\\)", " women ")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("rpa", "South Africa");
        m.put("republika poludniowej afryki", "South Africa");
        m.put("south africa", "South Africa");
        m.put("korea pld", "South Korea");
        m.put("korea poludniowa", "South Korea");
        m.put("korea poludniowej", "South Korea");
        m.put("poludniowa korea", "South Korea");
        m.put("south korea", "South Korea");
        m.put("korea pn", "North Korea");
        m.put("korea polnocna", "North Korea");
        m.put("polnocna korea", "North Korea");
        m.put("niemcy", "Germany");
        m.put("germany", "Germany");
        m.put("norwegia", "Norway");
        m.put("norway", "Norway");
        m.put("szwecja", "Sweden");
        m.put("sweden", "Sweden");
        m.put("belgia", "Belgium");
        m.put("belgium", "Belgium");
        m.put("serbia", "Serbia");
        m.put("polska", "Poland");
        m.put("poland", "Poland");
        m.put("francja", "France");
        m.put("france", "France");
        m.put("wybrzeze kosci sloniowej", "Ivory Coast");
        m.put("ivory coast", "Ivory Coast");
        m.put("walia", "Wales");
        m.put("wales", "Wales");
        m.put("czarnogora", "Montenegro");
        m.put("montenegro", "Montenegro");
        m.put("japonia", "Japan");
        m.put("japan", "Japan");
        m.put("holandia", "Netherlands");
        m.put("netherlands", "Netherlands");
        m.put("hiszpania", "Spain");
        m.put("spain", "Spain");
        m.put("ekwador", "Ecuador");
        m.put("ecuador", "Ecuador");
        m.put("curacao", "Curacao");
        m.put("portugalia", "Portugal");
        m.put("portugal", "Portugal");
        m.put("uzbekistan", "Uzbekistan");
        m.put("tunezja", "Tunisia");
        m.put("tunisia", "Tunisia");
        m.put("arabia saudyjska", "Saudi Arabia");
        m.put("saudi arabia", "Saudi Arabia");
        m.put("wyspy zielonego przyladka", "Cape Verde");
        m.put("cape verde", "Cape Verde");
        m.put("turcja", "Turkey");
        m.put("turkey", "Turkey");
        m.put("chorwacja", "Croatia");
        m.put("croatia", "Croatia");
        m.put("slowenia", "Slovenia");
        m.put("slovenia", "Slovenia");
        m.put("cypr", "Cyprus");
        m.put("cyprus", "Cyprus");
        m.put("dania", "Denmark");
        m.put("denmark", "Denmark");
        m.put("kanada", "Canada");
        m.put("canada", "Canada");
        m.put("usa", "United States");
        m.put("stany zjednoczone", "United States");
        m.put("meksyk", "Mexico");
        m.put("mexico", "Mexico");
        m.put("paragwaj", "Paraguay");
        m.put("paraguay", "Paraguay");
        m.put("brazylia", "Brazil");
        m.put("brazil", "Brazil");
        m.put("wlochy", "Italy");
        m.put("italy", "Italy");
        m.put("czechy", "Czech Republic");
        m.put("czech republic", "Czech Republic");
        m.put("irak", "Iraq");
        m.put("iraq", "Iraq");
        m.put("maroko", "Morocco");
        m.put("morocco", "Morocco");
        m.put("kolumbia", "Colombia");
        m.put("colombia", "Colombia");
        m.put("kostaryka", "Costa Rica");
        m.put("costa rica", "Costa Rica");
        m.put("argentina", "Argentina");
        m.put("argentyna", "Argentina");
        m.put("jordania", "Jordan");
        m.put("jordan", "Jordan");
        m.put("real madryt", "Real Madrid");
        m.put("real madrid", "Real Madrid");
        m.put("legia warszawa", "Legia Warszawa");
        m.put("lech poznan", "Lech Poznan");
        m.put("lechia gdansk", "Lechia Gdansk");
        m.put("pogon szczecin", "Pogon Szczecin");
        m.put("gornik leczna", "Gornik Leczna");
        m.put("arka gdynia", "Arka Gdynia");
        m.put("brighton", "Brighton");
        m.put("manchester united", "Manchester United");
        m.put("manchester city", "Manchester City");
        return m;
    }
}
