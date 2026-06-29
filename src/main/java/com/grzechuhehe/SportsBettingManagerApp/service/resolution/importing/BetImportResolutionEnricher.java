package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.AtomicCondition;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.CompositeSelectionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class BetImportResolutionEnricher {

    private static final String[] OUTRIGHT_KEYWORDS = {
            "mistrzostwa", "awansuje", "klasyfikacja"
    };

    private final CompositeSelectionParser compositeSelectionParser;
    private final ObjectMapper objectMapper;
    private final MarketTypeInferrer marketTypeInferrer;

    public void enrich(Bet bet) {
        normalizeEventNameSeparators(bet);

        if (isOutright(bet)) {
            bet.setMarketType(MarketType.OUTRIGHT);
        } else if (bet.getMarketType() == null || bet.getMarketType() == MarketType.OTHER) {
            MarketType inferred = marketTypeInferrer.infer(bet);
            if (inferred != null) {
                bet.setMarketType(inferred);
            }
        }

        enrichBuilderConditions(bet);
    }

    private void normalizeEventNameSeparators(Bet bet) {
        if (bet.getEventName() == null) {
            return;
        }
        String normalized = bet.getEventName()
                .replaceAll("(?i)\\s+vs\\s+", " - ")
                .replaceAll("(?i)\\s+v\\.\\s+", " - ");
        if (!normalized.equals(bet.getEventName())) {
            bet.setEventName(normalized);
        }
    }

    private boolean isOutright(Bet bet) {
        String combined = ((bet.getEventName() == null ? "" : bet.getEventName())
                + " " + (bet.getSelection() == null ? "" : bet.getSelection()))
                .toLowerCase(Locale.ROOT);
        for (String keyword : OUTRIGHT_KEYWORDS) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void enrichBuilderConditions(Bet bet) {
        if (bet.getBuilderConditionsJson() != null && !bet.getBuilderConditionsJson().isBlank()) {
            return;
        }
        if (!isBetBuilderSelection(bet)) {
            return;
        }
        compositeSelectionParser.parseComplete(bet.getSelection())
                .filter(conditions -> !conditions.isEmpty())
                .ifPresent(conditions -> bet.setBuilderConditionsJson(serializeConditions(conditions)));
    }

    private boolean isBetBuilderSelection(Bet bet) {
        String selection = bet.getSelection();
        if (selection == null || selection.isBlank()) {
            return false;
        }
        String lower = selection.toLowerCase(Locale.ROOT);
        if (lower.contains("bet builder") || lower.contains("betbuilder")) {
            return true;
        }
        return compositeSelectionParser.parseComplete(selection)
                .map(conditions -> conditions.size() >= 2)
                .orElse(false);
    }

    private String serializeConditions(List<AtomicCondition> conditions) {
        try {
            return objectMapper.writeValueAsString(conditions);
        } catch (JsonProcessingException e) {
            log.warn("Nie udało się zserializować builderConditions: {}", e.getMessage());
            return null;
        }
    }
}
