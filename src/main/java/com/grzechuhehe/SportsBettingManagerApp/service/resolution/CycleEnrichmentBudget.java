package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import java.util.concurrent.atomic.AtomicInteger;

public class CycleEnrichmentBudget {

    private final int maxCalls;
    private final AtomicInteger used = new AtomicInteger(0);

    public CycleEnrichmentBudget(int maxCalls) {
        this.maxCalls = Math.max(maxCalls, 0);
    }

    public boolean tryConsume() {
        while (true) {
            int current = used.get();
            if (current >= maxCalls) {
                return false;
            }
            if (used.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    public int remaining() {
        return Math.max(0, maxCalls - used.get());
    }
}
