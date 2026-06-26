package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.SofaScoreQueryCache;
import com.grzechuhehe.SportsBettingManagerApp.repository.SofaScoreQueryCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SofaScoreCacheServiceTest {

    @Mock
    private SofaScoreQueryCacheRepository cacheRepository;

    private SofaScoreCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SofaScoreCacheService(cacheRepository, new ObjectMapper());
        ReflectionTestUtils.setField(cacheService, "cacheTtlHours", 72);
    }

    @Test
    void shouldReturnFreshCachedEventsForKnownQuery() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 12, 0);
        String query = "Croatia Slovenia";
        String hash = SofaScoreCacheService.sha256(SofaScoreCacheService.normalize(query));

        SofaScoreEventDto event = new SofaScoreEventDto();
        event.setType("match");
        event.setHomeTeam("Croatia");
        event.setAwayTeam("Slovenia");
        event.setUrl("https://www.sofascore.com/croatia-slovenia#id:1");

        String payload = new ObjectMapper().writeValueAsString(List.of(event));
        SofaScoreQueryCache row = new SofaScoreQueryCache();
        row.setQueryHash(hash);
        row.setQueryText(query);
        row.setPayloadJson(payload);
        row.setExpiresAt(now.plusHours(24));

        when(cacheRepository.findByQueryHashInAndExpiresAtAfter(anyCollection(), eq(now)))
                .thenReturn(List.of(row));

        SofaScoreCacheService.CacheLookupResult hit = cacheService.getFresh(List.of(query), now);

        assertEquals(1, hit.events().size());
        assertEquals("Croatia", hit.events().getFirst().getHomeTeam());
        assertTrue(hit.missingQueries().isEmpty());
    }

    @Test
    void shouldReportMissingQueriesWhenCacheExpiredOrAbsent() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 12, 0);
        when(cacheRepository.findByQueryHashInAndExpiresAtAfter(anyCollection(), eq(now)))
                .thenReturn(List.of());

        SofaScoreCacheService.CacheLookupResult miss =
                cacheService.getFresh(List.of("Germany Norway"), now);

        assertTrue(miss.events().isEmpty());
        assertEquals(List.of("Germany Norway"), List.copyOf(miss.missingQueries()));
    }

    @Test
    void shouldUpsertCacheRowsOnPutAll() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 12, 0);
        SofaScoreEventDto event = new SofaScoreEventDto();
        event.setType("match");
        event.setUrl("https://www.sofascore.com/x#id:2");

        when(cacheRepository.findByQueryHash(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.save(any(SofaScoreQueryCache.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, List<SofaScoreEventDto>> byQuery = new LinkedHashMap<>();
        byQuery.put("Legia Lech", List.of(event));

        cacheService.putAll(byQuery, now);

        ArgumentCaptor<SofaScoreQueryCache> captor = ArgumentCaptor.forClass(SofaScoreQueryCache.class);
        verify(cacheRepository).save(captor.capture());
        SofaScoreQueryCache saved = captor.getValue();
        assertEquals("Legia Lech", saved.getQueryText());
        assertEquals(1, saved.getEventCount());
        assertEquals(now.plusHours(72), saved.getExpiresAt());
        assertEquals("APIFY", saved.getSource());
    }

    @Test
    void shouldPurgeExpiredRowsOnSchedule() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 4, 30);
        cacheService.purgeExpired();
        verify(cacheRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
