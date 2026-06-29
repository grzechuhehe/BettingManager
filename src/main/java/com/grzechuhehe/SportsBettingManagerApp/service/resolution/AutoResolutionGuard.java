package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single source of truth for auto-resolution throttling: a running-lock plus a
 * post-success cooldown. Both the scheduled cycle and the manual trigger go
 * through this guard so they cannot overlap (each Apify call costs money).
 *
 * In-memory / per-JVM: correct for the current single-instance deploy. For
 * horizontal scaling, back this with a DB lock (see plan Phase 5).
 */
@Component
public class AutoResolutionGuard {

    public enum Acquisition { ACQUIRED, BUSY, COOLDOWN }

    public record AcquireResult(Acquisition status, long remainingMinutes) {}

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastFinishedAtMs = new AtomicLong(0);

    /**
     * @param cooldownMinutes minimum gap after the last successful run
     * @param force when true, ignores cooldown (still blocked by an in-flight run)
     */
    public AcquireResult tryAcquire(long cooldownMinutes, boolean force) {
        if (!force) {
            long last = lastFinishedAtMs.get();
            if (last > 0) {
                long cooldownMs = cooldownMinutes * 60_000L;
                long elapsed = System.currentTimeMillis() - last;
                if (elapsed < cooldownMs) {
                    long remaining = (cooldownMs - elapsed + 59_999) / 60_000;
                    return new AcquireResult(Acquisition.COOLDOWN, remaining);
                }
            }
        }
        if (!running.compareAndSet(false, true)) {
            return new AcquireResult(Acquisition.BUSY, 0);
        }
        return new AcquireResult(Acquisition.ACQUIRED, 0);
    }

    /** Releases the lock. Records the cooldown start only when the run succeeded. */
    public void release(boolean success) {
        if (success) {
            lastFinishedAtMs.set(System.currentTimeMillis());
        }
        running.set(false);
    }
}
