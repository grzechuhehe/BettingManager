package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import java.time.LocalDateTime;

public record ResolutionRunConfig(
        QueueMode queueMode,
        int limit,
        LocalDateTime placedBeforeCutoff,
        int dateWindowDays
) {
    public enum QueueMode {
        NEWEST_FIRST,
        OLDEST_BEFORE_CUTOFF
    }

    public static ResolutionRunConfig defaultNewest(int limit, int dateWindowDays) {
        return new ResolutionRunConfig(QueueMode.NEWEST_FIRST, limit, null, dateWindowDays);
    }

    public static ResolutionRunConfig oldestBefore(LocalDateTime cutoff, int limit, int dateWindowDays) {
        return new ResolutionRunConfig(QueueMode.OLDEST_BEFORE_CUTOFF, limit, cutoff, dateWindowDays);
    }
}
