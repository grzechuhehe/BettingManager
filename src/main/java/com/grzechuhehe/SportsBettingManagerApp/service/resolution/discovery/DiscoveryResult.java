package com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;

import java.util.List;
import java.util.Set;

public record DiscoveryResult(
        List<SofaScoreEventDto> events,
        int apifyCalls,
        Set<Long> fetchedBetIds,
        int apifyFailures,
        int cacheHits) {

    public static DiscoveryResult empty() {
        return new DiscoveryResult(List.of(), 0, Set.of(), 0, 0);
    }
}
