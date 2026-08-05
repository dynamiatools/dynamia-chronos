package tools.dynamia.chronos;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.commons.SimpleCache;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.integration.sterotypes.Service;

import java.time.Duration;
import java.time.Instant;

/*
Main schedule configurer
 */
@Service
public class CronJobScheduler implements SchedulingConfigurer {

    /**
     * Floor applied to a job's serverTimeout when computing how long its
     * distributed lock is held at most. Keeps very short timeouts from
     * producing a lock window so tight that clock drift between instances
     * could let a second instance start the job before the first one is
     * done recording its result.
     */
    private static final Duration MIN_LOCK_AT_MOST_FOR = Duration.ofSeconds(30);

    /**
     * Extra time added on top of the job's serverTimeout so the lock covers
     * not just the HTTP/TCP call itself but also persisting the CronJobLog
     * and firing notifications afterward.
     */
    private static final Duration LOCK_SAFETY_MARGIN = Duration.ofSeconds(15);

    /**
     * Mirrors {@code CronJobsServiceImpl.MAX_BACKOFF_MS}: the cap applied to
     * each retry's exponential backoff. Used here as a conservative
     * upper bound on total retry wait time, not as the real backoff value.
     */
    private static final Duration MAX_RETRY_BACKOFF = Duration.ofSeconds(60);

    private SimpleCache<Long, ScheduledTask> tasksCache = new SimpleCache<>();
    private LoggingService logger = new SLF4JLoggingService(CronJobScheduler.class);

    private final CronJobsService cronJobsService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private ScheduledTaskRegistrar taskRegistrar;

    public CronJobScheduler(CronJobsService cronJobsService, LockProvider lockProvider) {
        this.cronJobsService = cronJobsService;
        this.lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        logger.info("Starting cron job scheduler");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
        logger.info("Registering cron jobs");
        cronJobsService.getActiveCronJobs().forEach(this::scheduleJob);
    }

    /**
     * Cancel all cron jobs and start again
     */
    public void reload() {
        cancelAll();
        configureTasks(taskRegistrar);
    }

    /**
     * Schedul a cron job
     *
     * @param cronJob
     */
    public void scheduleJob(CronJob cronJob) {
        cancel(cronJob);

        if (cronJob.isActive()) {
            logger.info("Scheduling cron job " + cronJob.getId() + " - " + cronJob.getName() + " with " + cronJob.getCronExpression());
            ScheduledTask scheduledTask = taskRegistrar.scheduleCronTask(new CronTask(
                    () -> executeWithLock(cronJob)
                    , cronJob.getCronExpression()
            ));
            tasksCache.add(cronJob.getId(), scheduledTask);
        }
    }

    /**
     * Runs the job under a distributed lock keyed by the cron job id, so
     * when Chronos runs with multiple replicas only the instance that wins
     * the lock actually executes it. Instances that lose the race skip the
     * trigger silently instead of running it a second time.
     *
     * @param cronJob
     */
    void executeWithLock(CronJob cronJob) {
        var lockConfiguration = new LockConfiguration(Instant.now(), "cronjob-" + cronJob.getId(), computeLockAtMostFor(cronJob), Duration.ZERO);
        lockingTaskExecutor.executeWithLock((Runnable) () -> cronJobsService.execute(cronJob), lockConfiguration);
    }

    /**
     * Worst-case time a job's execution (including retries, see
     * {@code CronJob.maxRetries}/{@code retryBackoffMs}) could take, plus a
     * safety margin — this must never expire before a legitimate retry
     * sequence finishes, or a second instance could pick up the same job
     * mid-retry. Deliberately an overestimate: every attempt is assumed to
     * take the full serverTimeout and every backoff hits the cap, even
     * though real runs usually finish well before that.
     */
    private static Duration computeLockAtMostFor(CronJob cronJob) {
        int totalAttempts = cronJob.getMaxRetries() + 1;
        var worstCase = cronJob.getServerTimeout().multipliedBy(totalAttempts)
                .plus(MAX_RETRY_BACKOFF.multipliedBy(cronJob.getMaxRetries()))
                .plus(LOCK_SAFETY_MARGIN);
        return worstCase.compareTo(MIN_LOCK_AT_MOST_FOR) > 0 ? worstCase : MIN_LOCK_AT_MOST_FOR;
    }

    /**
     * Cancell all scheduled cron jobs
     */
    public void cancelAll() {
        logger.info("Canceling all cron jobs");
        tasksCache.forEach((id, task) -> task.cancel());
        tasksCache.clear();
    }

    /**
     * Cancel specific cron job
     *
     * @param cronJob
     * @return true if job was found and cancel
     */
    public boolean cancel(CronJob cronJob) {
        var task = findScheduleTask(cronJob);
        if (task != null) {
            logger.info("Canceling cron job " + cronJob.getId() + " - " + cronJob.getName());
            task.cancel();
            tasksCache.remove(cronJob.getId());
            return true;
        }
        return false;
    }

    /**
     * Find associated schedule task to cron job
     *
     * @param cronJob
     * @return task
     */
    public ScheduledTask findScheduleTask(CronJob cronJob) {
        return tasksCache.get(cronJob.getId());
    }
}
