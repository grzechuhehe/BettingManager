package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

public record ResolutionCycleMetrics(
        String cycleId,
        int eligible,
        int discoveryCalls,
        int enrichmentCalls,
        int settled,
        int belowThreshold,
        int noMatch,
        double estimatedCostUsd) {}
