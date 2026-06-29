package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.service.resolution.AutoResolutionGuard.Acquisition;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.AutoResolutionGuard.AcquireResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoResolutionGuardTest {

    @Test
    void acquiresWhenIdle() {
        AutoResolutionGuard guard = new AutoResolutionGuard();
        AcquireResult result = guard.tryAcquire(60, false);
        assertThat(result.status()).isEqualTo(Acquisition.ACQUIRED);
    }

    @Test
    void reportsBusyWhenAlreadyRunning() {
        AutoResolutionGuard guard = new AutoResolutionGuard();
        guard.tryAcquire(60, false); // not released
        AcquireResult result = guard.tryAcquire(60, false);
        assertThat(result.status()).isEqualTo(Acquisition.BUSY);
    }

    @Test
    void reportsCooldownAfterSuccessfulRelease() {
        AutoResolutionGuard guard = new AutoResolutionGuard();
        guard.tryAcquire(60, false);
        guard.release(true); // success records finish time
        AcquireResult result = guard.tryAcquire(60, false);
        assertThat(result.status()).isEqualTo(Acquisition.COOLDOWN);
        assertThat(result.remainingMinutes()).isBetween(1L, 60L);
    }

    @Test
    void noCooldownAfterFailedRelease() {
        AutoResolutionGuard guard = new AutoResolutionGuard();
        guard.tryAcquire(60, false);
        guard.release(false); // failure does NOT start cooldown
        AcquireResult result = guard.tryAcquire(60, false);
        assertThat(result.status()).isEqualTo(Acquisition.ACQUIRED);
    }

    @Test
    void forceBypassesCooldownButNotBusy() {
        AutoResolutionGuard guard = new AutoResolutionGuard();
        guard.tryAcquire(60, false);
        guard.release(true);
        AcquireResult forced = guard.tryAcquire(60, true);
        assertThat(forced.status()).isEqualTo(Acquisition.ACQUIRED);
    }
}
