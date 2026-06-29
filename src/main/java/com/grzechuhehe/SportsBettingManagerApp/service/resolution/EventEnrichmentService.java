package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventEnrichmentService {

    private static final Pattern STATS_SELECTION = Pattern.compile(
            "foul|strzał|shot|card|kartk|asów|aces",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final ApifySofaScoreClient apifyClient;

    @Value("${bet.resolution.enrichment-enabled:false}")
    private boolean enrichmentEnabled;

    @Value("${bet.resolution.enrichment-min-confidence:0.80}")
    private double enrichmentMinConfidence;

    public SofaScoreEventDto enrichIfNeeded(
            Bet bet,
            SofaScoreEventDto event,
            double confidence,
            CycleEnrichmentBudget budget) {
        if (!enrichmentEnabled) {
            return event;
        }
        if (confidence < enrichmentMinConfidence) {
            return event;
        }
        if (event == null || event.getUrl() == null || event.getUrl().isBlank()) {
            return event;
        }
        if (!selectionNeedsStats(bet.getSelection())) {
            return event;
        }
        if (budget == null || !budget.tryConsume()) {
            log.info("Zakład {}: limit wzbogacenia statystyk na cykl wyczerpany", bet.getId());
            return event;
        }

        Optional<SofaScoreEventDto> enriched = apifyClient.fetchEventDetails(event.getUrl());
        if (enriched.isEmpty()) {
            log.warn("Zakład {}: Apify nie zwrócił statystyk dla {}", bet.getId(), event.getUrl());
            return event;
        }

        log.info("Zakład {}: wzbogacono mecz statystykami Apify ({})", bet.getId(), event.getUrl());
        return merge(event, enriched.get());
    }

    private static boolean selectionNeedsStats(String selection) {
        if (selection == null || selection.isBlank()) {
            return false;
        }
        return STATS_SELECTION.matcher(selection).find();
    }

    private static SofaScoreEventDto merge(SofaScoreEventDto base, SofaScoreEventDto enriched) {
        if (enriched.getStatistics() != null) {
            base.setStatistics(enriched.getStatistics());
        }
        if (enriched.getEventId() != null) {
            base.setEventId(enriched.getEventId());
        }
        return base;
    }
}
