package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import java.util.Locale;
import java.util.Map;

/** Czyta liczby ze zagnieżdżonej mapy statistics z Apify/SofaScore. */
final class SofaScoreStatisticsReader {

    private SofaScoreStatisticsReader() {}

    static Integer teamTotal(Map<String, Object> statistics, String side, String... keyFragments) {
        if (statistics == null || side == null) {
            return null;
        }
        Object sideNode = statistics.get(side);
        if (sideNode instanceof Map<?, ?> sideMap) {
            Integer fromSide = findIntInMap(sideMap, keyFragments);
            if (fromSide != null) {
                return fromSide;
            }
        }
        return findIntInMap(statistics, keyFragments);
    }

    static Integer matchTotal(Map<String, Object> statistics, String... keyFragments) {
        if (statistics == null) {
            return null;
        }
        Integer home = teamTotal(statistics, "home", keyFragments);
        Integer away = teamTotal(statistics, "away", keyFragments);
        if (home != null && away != null) {
            return home + away;
        }
        return findIntInMap(statistics, keyFragments);
    }

    @SuppressWarnings("unchecked")
    private static Integer findIntInMap(Map<?, ?> map, String... keyFragments) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            Object value = entry.getValue();
            if (matchesKey(key, keyFragments)) {
                Integer parsed = toInt(value);
                if (parsed != null) {
                    return parsed;
                }
            }
            if (value instanceof Map<?, ?> nested) {
                Integer nestedResult = findIntInMap(nested, keyFragments);
                if (nestedResult != null) {
                    return nestedResult;
                }
            }
        }
        return null;
    }

    private static boolean matchesKey(String key, String... fragments) {
        for (String fragment : fragments) {
            if (key.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return (int) Double.parseDouble(text.replace(',', '.'));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
