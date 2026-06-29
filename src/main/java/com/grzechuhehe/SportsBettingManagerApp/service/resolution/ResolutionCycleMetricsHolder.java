package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ResolutionCycleMetricsHolder {

    private volatile ResolutionCycleMetrics last;

    public void setLast(ResolutionCycleMetrics metrics) {
        this.last = metrics;
    }

    public Optional<ResolutionCycleMetrics> getLast() {
        return Optional.ofNullable(last);
    }
}
