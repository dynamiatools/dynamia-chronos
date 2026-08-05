package tools.dynamia.chronos.services.impl;

import io.micrometer.core.instrument.MeterRegistry;
import tools.dynamia.chronos.ChronosHttpResponse;
import tools.dynamia.chronos.CronJobHttpRequestExecutor;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.listeners.CronJobExecutionListener;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.MapBuilder;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.domain.query.QueryParameters;
import tools.dynamia.domain.services.AbstractService;
import tools.dynamia.integration.Containers;
import tools.dynamia.integration.sterotypes.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Service
public class CronJobsServiceImpl extends AbstractService implements CronJobsService {

    /**
     * Safety valve for the exponential backoff between retries, regardless
     * of how a job's retryBackoffMs/maxRetries are configured.
     */
    private static final long MAX_BACKOFF_MS = 60_000;

    private static final String EXECUTIONS_METRIC = "chronos_cronjob_executions_total";
    private static final String DURATION_METRIC = "chronos_cronjob_execution_duration_seconds";

    private final ProjectService projectService;
    private final MeterRegistry meterRegistry;

    public CronJobsServiceImpl(ProjectService projectService, MeterRegistry meterRegistry) {
        this.projectService = projectService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<CronJob> getActiveCronJobs() {
        return crudService().find(CronJob.class, "active", true);
    }

    public List<CronJob> getFailingCronJobs() {
        return crudService().find(CronJob.class, QueryParameters.with("active", true)
                .add("status", FAILING_STATUS));
    }

    @Override
    public void execute(CronJob cronJob) {
        execute(cronJob, false);
    }


    /**
     * Execute a cron job
     *
     * @param cronJob  the cron job
     * @param testMode if is in test mode
     * @return the cron job log
     */
    private CronJobLog execute(CronJob cronJob, boolean testMode) {
        fireBeforeExecutionListener(cronJob);

        var prefix = testMode ? "[TEST] " : "[JOB] ";
        try {
            var executor = new CronJobHttpRequestExecutor(cronJob, projectService.getVariablesFor(cronJob),
                    message -> log(prefix + message));

            // Retries are never applied in test mode: "Test" is meant to give
            // immediate feedback on a single attempt, not to wait out backoff.
            int maxAttempts = testMode ? 1 : cronJob.getMaxRetries() + 1;
            var result = runWithRetry(executor::execute, maxAttempts, cronJob.getRetryBackoffMs(),
                    (attempt, backoff) -> log(prefix + "Attempt " + attempt + "/" + maxAttempts
                            + " failed for [" + cronJob.getName() + "], retrying in " + backoff + "ms"));
            ChronosHttpResponse log = result.response();

            if (log instanceof CronJobLog cl) {
                cl.setTest(testMode);
                cl.setAttemptCount(result.attempts());
            }

            final ChronosHttpResponse finalLog = log;
            crudService().executeWithinTransaction(() -> {
                crudService().increaseCounter(cronJob, "executionsCount");
                crudService().batchUpdate(CronJob.class,
                        MapBuilder.put(
                                "lastExecution", LocalDateTime.now(),
                                "status", finalLog.isFail() ? FAILING_STATUS : OK_STATUS
                        ),
                        QueryParameters.with("id", cronJob.getId()));
                crudService().create(finalLog);
            });

            recordMetrics(cronJob, log, testMode);
            fireAfterExecutionListener(cronJob, log);
            return (CronJobLog) log;
        } catch (Exception e) {
            log(prefix + "Error executing cron job: " + e.getMessage());
            CronJobLog errorLog = new CronJobLog(cronJob, false, true);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("exception", e.getClass().getSimpleName());
            errorDetails.put("message", e.getMessage());
            errorDetails.put("cause", e.getCause() != null ? e.getCause().toString() : null);
            errorLog.setResponse(StringPojoParser.convertMapToJson(errorDetails));
            errorLog.setTest(testMode);
            errorLog.setDetails("Error executing cron job: " + e.getMessage());
            errorLog.setStatus("ERROR");
            errorLog.setStatusCode(500);
            crudService().create(errorLog);
            recordMetrics(cronJob, errorLog, testMode);
            fireAfterExecutionListener(cronJob, errorLog);
            return errorLog;
        }
    }

    /**
     * Outcome of {@link #runWithRetry}: the final response (success, or the
     * last failure once attempts were exhausted) and how many attempts it took.
     */
    record RetryResult(ChronosHttpResponse response, int attempts) {
    }

    /**
     * Runs {@code action} up to {@code maxAttempts} times, stopping as soon
     * as one succeeds. Pure retry/backoff logic, decoupled from persistence
     * and from the HTTP executor so it can be unit tested on its own.
     *
     * @param action      performs one attempt and returns its response
     * @param maxAttempts total attempts allowed (1 = no retry)
     * @param backoffMs   base backoff between attempts; see {@link #nextBackoffMs}
     * @param onRetry     notified with (attempt, backoffMs actually waited) before each retry sleep
     */
    static RetryResult runWithRetry(Supplier<ChronosHttpResponse> action, int maxAttempts,
                                     long backoffMs, BiConsumer<Integer, Long> onRetry) {
        ChronosHttpResponse response;
        int attempt = 0;
        do {
            attempt++;
            response = action.get();
            if (!response.isFail() || attempt >= maxAttempts) {
                break;
            }
            long backoff = nextBackoffMs(backoffMs, attempt);
            onRetry.accept(attempt, backoff);
            sleepQuietly(backoff);
        } while (attempt < maxAttempts);
        return new RetryResult(response, attempt);
    }

    /**
     * Exponential backoff: backoffMs * 2^(attempt-1), e.g. with backoffMs=1000
     * attempts 1,2,3 wait 1s, 2s, 4s before the next try. Capped so a
     * misconfigured job can't stall the scheduler thread for too long.
     *
     * @param backoffMs base backoff configured on the job; 0 means no wait
     * @param attempt   the attempt number that just failed (1-based)
     */
    static long nextBackoffMs(long backoffMs, int attempt) {
        if (backoffMs <= 0) {
            return 0;
        }
        long exponential = backoffMs * (1L << Math.min(attempt - 1, 10));
        return Math.min(exponential, MAX_BACKOFF_MS);
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Records execution metrics for Prometheus scraping (see {@code /actuator/prometheus}
     * on the management port). Test-mode ("Test" action) runs are excluded so manual
     * poking around doesn't skew a project's real success-rate/duration numbers.
     */
    private void recordMetrics(CronJob cronJob, ChronosHttpResponse log, boolean testMode) {
        if (testMode) {
            return;
        }
        String project = cronJob.getProject() != null ? cronJob.getProject().getName() : "unknown";
        meterRegistry.counter(EXECUTIONS_METRIC,
                "project", project,
                "result", log.isFail() ? "failure" : "success"
        ).increment();

        if (log.getDuration() != null) {
            meterRegistry.timer(DURATION_METRIC, "project", project)
                    .record(log.getDuration());
        }
    }

    private static void fireAfterExecutionListener(CronJob cronJob, ChronosHttpResponse log) {
        if (log instanceof CronJobLog cl) {
            Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.afterExecution(cronJob, cl));
        }
    }

    private static void fireBeforeExecutionListener(CronJob cronJob) {
        Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.beforeExecution(cronJob));
    }

    @Override
    public CronJobLog test(CronJob cronJob) {
        return execute(cronJob, true);
    }

    @Override
    public List<CronJobLog> getLatestLogs(CronJob cronJob) {
        return crudService().find(CronJobLog.class, QueryParameters.with("cronJob", cronJob)
                .orderBy("id", false)
                .setMaxResults(100));
    }

    @Override
    public long getActiveCronJobsCount() {
        return crudService().count(CronJob.class, QueryParameters.with("active", true));
    }

    @Override
    public long countCronJobsByStatus(String status) {
        return crudService().count(CronJob.class, QueryParameters.with("active", true)
                .add("status", status));
    }
}
