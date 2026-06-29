package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalizes tennis/darts doubles event names (player1/player2) into Apify search queries.
 */
@Component
public class DoublesNameNormalizer {

    private static final Pattern VS_SPLIT = Pattern.compile("\\s+vs\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH_SPLIT = Pattern.compile("\\s+-\\s+");

    public Optional<String> toApifyQuery(String eventName) {
        if (eventName == null || eventName.isBlank() || !eventName.contains("/")) {
            return Optional.empty();
        }

        String[] sides = splitTwoSides(eventName.trim());
        if (sides == null || sides.length != 2) {
            return Optional.empty();
        }

        Optional<String> homeSurname = extractRepresentativeSurname(sides[0].trim());
        Optional<String> awaySurname = extractRepresentativeSurname(sides[1].trim());
        if (homeSurname.isEmpty() || awaySurname.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(homeSurname.get() + " " + awaySurname.get());
    }

    private String[] splitTwoSides(String eventName) {
        if (VS_SPLIT.matcher(eventName).find()) {
            return VS_SPLIT.split(eventName, 2);
        }
        if (DASH_SPLIT.matcher(eventName).find()) {
            return DASH_SPLIT.split(eventName, 2);
        }
        return null;
    }

    private Optional<String> extractRepresentativeSurname(String side) {
        String[] players = side.split("/");
        if (players.length == 0) {
            return Optional.empty();
        }
        return extractSurnameFromPlayer(players[0].trim());
    }

    private Optional<String> extractSurnameFromPlayer(String player) {
        if (player.isBlank()) {
            return Optional.empty();
        }
        int dot = player.lastIndexOf('.');
        if (dot >= 0 && dot < player.length() - 1) {
            return Optional.of(player.substring(dot + 1).trim());
        }
        String[] parts = player.trim().split("\\s+");
        String lastWord = parts[parts.length - 1];
        if (lastWord.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(lastWord);
    }
}
