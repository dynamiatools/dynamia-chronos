package tools.dynamia.chronos.services.impl;

import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.ChronosHttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure retry/backoff logic in {@link CronJobsServiceImpl},
 * decoupled from persistence and the HTTP executor (see {@code runWithRetry}).
 */
class CronJobsServiceImplRetryTest {

    private static ChronosHttpResponse response(boolean fail) {
        var r = new ChronosHttpResponse();
        r.setFail(fail);
        return r;
    }

    @Test
    void noRetryConfiguredRunsOnlyOnceEvenOnFailure() {
        AtomicInteger calls = new AtomicInteger();
        var result = CronJobsServiceImpl.runWithRetry(
                () -> {
                    calls.incrementAndGet();
                    return response(true);
                },
                1, 0, (a, b) -> {
                });

        assertEquals(1, calls.get());
        assertEquals(1, result.attempts());
        assertTrue(result.response().isFail());
    }

    @Test
    void succeedsBeforeExhaustingRetries() {
        AtomicInteger calls = new AtomicInteger();
        var result = CronJobsServiceImpl.runWithRetry(
                () -> {
                    int n = calls.incrementAndGet();
                    return response(n < 3); // fails on attempt 1 and 2, succeeds on 3
                },
                5, 0, (a, b) -> {
                });

        assertEquals(3, calls.get());
        assertEquals(3, result.attempts());
        assertFalse(result.response().isFail());
    }

    @Test
    void exhaustsAllRetriesWhenAlwaysFailing() {
        AtomicInteger calls = new AtomicInteger();
        List<Integer> retriedAttempts = new ArrayList<>();

        var result = CronJobsServiceImpl.runWithRetry(
                () -> {
                    calls.incrementAndGet();
                    return response(true);
                },
                3, 0, (attempt, backoff) -> retriedAttempts.add(attempt));

        assertEquals(3, calls.get());
        assertEquals(3, result.attempts());
        assertTrue(result.response().isFail());
        // onRetry fires once per failed attempt that still has a retry left (attempts 1 and 2, not 3)
        assertEquals(List.of(1, 2), retriedAttempts);
    }

    @Test
    void backoffGrowsExponentiallyAndIsCapped() {
        assertEquals(0, CronJobsServiceImpl.nextBackoffMs(0, 1));
        assertEquals(1000, CronJobsServiceImpl.nextBackoffMs(1000, 1));
        assertEquals(2000, CronJobsServiceImpl.nextBackoffMs(1000, 2));
        assertEquals(4000, CronJobsServiceImpl.nextBackoffMs(1000, 3));
        assertEquals(60_000, CronJobsServiceImpl.nextBackoffMs(1000, 20), "must be capped at 60s");
    }
}
