package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

public record ResolutionHealthAlert(
        Level level,
        long successLast24h,
        long pendingLeaves,
        String message) {
    public enum Level { OK, WARN }
}
