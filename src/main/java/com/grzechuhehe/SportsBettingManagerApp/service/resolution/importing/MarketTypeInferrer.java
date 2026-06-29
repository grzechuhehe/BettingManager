package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MarketTypeInferrer {

    private final ResolutionNameTranslator nameTranslator;

    public MarketType infer(Bet bet) {
        if (bet.getEventName() == null) {
            return null;
        }
        if (nameTranslator.resolveQueryForApify(bet.getEventName()).isEmpty()) {
            return null;
        }
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        if (isTennisBet(bet)) {
            if (selection.matches(".*\\([+-]?\\d+(?:[.,]\\d+)?\\).*")) {
                return MarketType.HANDICAP;
            }
            return MarketType.MONEYLINE_12;
        }
        if (selection.matches(".*\\([+-]?\\d+(?:[.,]\\d+)?\\).*")) {
            return MarketType.HANDICAP;
        }
        if (selection.contains("over") || selection.contains("under")
                || selection.contains("powyzej") || selection.contains("powyżej")
                || selection.contains("ponizej") || selection.contains("poniżej")) {
            return MarketType.TOTALS_OVER_UNDER;
        }
        return MarketType.MONEYLINE_1X2;
    }

    private boolean isTennisBet(Bet bet) {
        if (bet.getSport() != null && !bet.getSport().isBlank()) {
            String sport = bet.getSport().toLowerCase(Locale.ROOT);
            return sport.contains("tennis") || sport.contains("tenis");
        }
        return looksLikeTwoPlayerNames(bet.getEventName());
    }

    private boolean looksLikeTwoPlayerNames(String eventName) {
        if (eventName == null) {
            return false;
        }
        return nameTranslator.parseTwoTeamSides(eventName)
                .map(sides -> sides.home().contains(",") && sides.away().contains(","))
                .orElse(false);
    }
}
