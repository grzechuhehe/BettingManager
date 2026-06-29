package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventEnrichmentServiceTest {

    private RecordingApifyClient apifyClient;
    private EventEnrichmentService service;

    @BeforeEach
    void setUp() {
        apifyClient = new RecordingApifyClient();
        service = new EventEnrichmentService(apifyClient);
        ReflectionTestUtils.setField(service, "enrichmentEnabled", true);
        ReflectionTestUtils.setField(service, "enrichmentMinConfidence", 0.80);
    }

    @Test
    void shouldSkipApifyCallWhenConfidenceBelowMinimum() {
        Bet bet = Bet.builder()
                .id(1L)
                .betType(BetType.SINGLE)
                .status(BetStatus.PENDING)
                .selection("Total fouls over 10.5")
                .build();
        SofaScoreEventDto event = baseEvent("https://www.sofascore.com/match/1");

        SofaScoreEventDto result = service.enrichIfNeeded(
                bet, event, 0.79, new CycleEnrichmentBudget(3));

        assertSame(event, result);
        assertNull(result.getStatistics());
        assertEquals(0, apifyClient.fetchCalls.get());
    }

    @Test
    void shouldFetchEventDetailsWhenConfidenceHighAndSelectionNeedsStats() {
        Bet bet = Bet.builder()
                .id(2L)
                .betType(BetType.SINGLE)
                .status(BetStatus.PENDING)
                .selection("Total fouls over 10.5")
                .build();
        SofaScoreEventDto event = baseEvent("https://www.sofascore.com/match/2");

        SofaScoreEventDto result = service.enrichIfNeeded(
                bet, event, 0.90, new CycleEnrichmentBudget(3));

        assertSame(event, result);
        assertEquals("evt-2", result.getEventId());
        assertNotNull(result.getStatistics());
        assertEquals(1, apifyClient.fetchCalls.get());
        assertEquals(event.getUrl(), apifyClient.lastUrl);
    }

    @Test
    void shouldSkipWhenSelectionDoesNotNeedStats() {
        Bet bet = Bet.builder()
                .id(3L)
                .betType(BetType.SINGLE)
                .status(BetStatus.PENDING)
                .selection("Legia Warszawa")
                .build();
        SofaScoreEventDto event = baseEvent("https://www.sofascore.com/match/3");

        SofaScoreEventDto result = service.enrichIfNeeded(
                bet, event, 0.95, new CycleEnrichmentBudget(3));

        assertSame(event, result);
        assertEquals(0, apifyClient.fetchCalls.get());
    }

    private static SofaScoreEventDto baseEvent(String url) {
        SofaScoreEventDto event = new SofaScoreEventDto();
        event.setType("match");
        event.setUrl(url);
        event.setHomeTeam("Home");
        event.setAwayTeam("Away");
        return event;
    }

    private static final class RecordingApifyClient extends ApifySofaScoreClient {
        final AtomicInteger fetchCalls = new AtomicInteger(0);
        String lastUrl;

        RecordingApifyClient() {
            super(
                    RestClient.builder(),
                    "https://api.apify.com/v2",
                    "test-token",
                    "abotapi~sofascore-scraper",
                    false,
                    0,
                    0);
        }

        @Override
        public Optional<SofaScoreEventDto> fetchEventDetails(String eventUrl) {
            fetchCalls.incrementAndGet();
            lastUrl = eventUrl;
            SofaScoreEventDto enriched = new SofaScoreEventDto();
            enriched.setStatistics(Map.of("fouls", Map.of("home", 12, "away", 9)));
            enriched.setEventId("evt-2");
            return Optional.of(enriched);
        }
    }
}
