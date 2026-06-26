package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class SofaScoreSportMapper {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("football", "football"),
            Map.entry("soccer", "football"),
            Map.entry("pilka nozna", "football"),
            Map.entry("futbol", "football"),
            Map.entry("basketball", "basketball"),
            Map.entry("koszykowka", "basketball"),
            Map.entry("tennis", "tennis"),
            Map.entry("tenis", "tennis"),
            Map.entry("ice hockey", "ice-hockey"),
            Map.entry("ice-hockey", "ice-hockey"),
            Map.entry("hockey", "ice-hockey"),
            Map.entry("hokej", "ice-hockey"),
            Map.entry("volleyball", "volleyball"),
            Map.entry("siatkowka", "volleyball"),
            Map.entry("handball", "handball"),
            Map.entry("pilka reczna", "handball"),
            Map.entry("baseball", "baseball"),
            Map.entry("bejsbol", "baseball"),
            Map.entry("american football", "american-football"),
            Map.entry("futbol amerykanski", "american-football"),
            Map.entry("darts", "darts"),
            Map.entry("snooker", "snooker"),
            Map.entry("mma", "mma"),
            Map.entry("boxing", "boxing"),
            Map.entry("boks", "boxing")
    );

    public Optional<String> map(String sport) {
        if (sport == null || sport.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(sport);
        if (ALIASES.containsKey(key)) {
            return Optional.of(ALIASES.get(key));
        }
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public List<String> resolveSportsForBets(List<Bet> bets, List<String> fallback) {
        Set<String> sports = new LinkedHashSet<>();
        for (Bet bet : bets) {
            map(bet.getSport()).ifPresent(sports::add);
        }
        if (sports.isEmpty()) {
            return fallback;
        }
        return new ArrayList<>(sports);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ł", "l") // NFD nie rozkłada ł — bez tego "piłka" → "pika"
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
