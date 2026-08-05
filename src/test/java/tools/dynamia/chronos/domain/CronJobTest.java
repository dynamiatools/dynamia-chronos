package tools.dynamia.chronos.domain;

import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.ChronosHttpMethod;
import tools.dynamia.chronos.HeadersProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronJobTest {

    @Test
    void defaultsAreSetCorrectly() {
        CronJob cronJob = new CronJob();
        assertTrue(cronJob.isActive());
        assertTrue(cronJob.isNotifyFails());
        assertFalse(cronJob.isNotifyExecutions());
        assertEquals(ChronosHttpMethod.GET, cronJob.getHttpMethod());
    }

    @Test
    void implementsHeadersProvider() {
        assertTrue(new CronJob() instanceof HeadersProvider);
    }

    @Test
    void headersRoundTripThroughJson() {
        CronJob cronJob = new CronJob();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", "abc123");
        headers.put("X-Trace-Id", "trace-1");

        cronJob.setHeaders(headers);

        assertEquals(headers, cronJob.getHeaders());
    }

    @Test
    void headersDefaultToEmptyMap() {
        CronJob cronJob = new CronJob();
        assertEquals(Map.of(), cronJob.getHeaders());
    }

    @Test
    void settingEmptyMapClearsPersistedHeaders() {
        CronJob cronJob = new CronJob();
        cronJob.setHeaders(Map.of("X-Api-Key", "abc123"));
        assertFalse(cronJob.getHeaders().isEmpty());

        cronJob.setHeaders(Map.of());

        assertEquals(Map.of(), cronJob.getHeaders());
    }

    @Test
    void settingNullHeadersClearsPersistedHeaders() {
        CronJob cronJob = new CronJob();
        cronJob.setHeaders(Map.of("a", "b"));

        cronJob.setHeaders(null);

        assertEquals(Map.of(), cronJob.getHeaders());
    }

    @Test
    void checkHeadersNullsOutBlankValue() throws Exception {
        CronJob cronJob = new CronJob();
        var field = CronJob.class.getDeclaredField("headers");
        field.setAccessible(true);
        field.set(cronJob, "   ");

        cronJob.checkHeaders();

        assertEquals(Map.of(), cronJob.getHeaders());
    }

    @Test
    void nextExecutionIsNoneWhenInactive() {
        CronJob cronJob = new CronJob();
        cronJob.setActive(false);
        cronJob.setCronExpression("0 0 * * * *");

        assertEquals("none", cronJob.getNextExecution());
    }

    @Test
    void retryDefaultsToNoRetry() {
        CronJob cronJob = new CronJob();
        assertEquals(0, cronJob.getMaxRetries());
        assertEquals(0, cronJob.getRetryBackoffMs());
    }

    @Test
    void negativeRetryValuesAreClampedToZero() {
        CronJob cronJob = new CronJob();
        cronJob.setMaxRetries(-5);
        cronJob.setRetryBackoffMs(-100);

        assertEquals(0, cronJob.getMaxRetries());
        assertEquals(0, cronJob.getRetryBackoffMs());
    }
}
