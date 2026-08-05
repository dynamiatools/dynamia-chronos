package tools.dynamia.chronos;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
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

import java.time.ZoneId;

/*
Main schedule configurer
 */
@Service
public class CronJobScheduler implements SchedulingConfigurer {

    private static final String NEXT_EXECUTION_METRIC = "chronos_cronjob_next_execution_timestamp_seconds";

    private SimpleCache<Long, ScheduledTask> tasksCache = new SimpleCache<>();
    private LoggingService logger = new SLF4JLoggingService(CronJobScheduler.class);

    private final CronJobsService cronJobsService;
    private final MultiGauge nextExecutionGauge;
    private ScheduledTaskRegistrar taskRegistrar;

    public CronJobScheduler(CronJobsService cronJobsService, MeterRegistry meterRegistry) {
        this.cronJobsService = cronJobsService;
        this.nextExecutionGauge = MultiGauge.builder(NEXT_EXECUTION_METRIC)
                .description("Epoch seconds of the next scheduled run for each active cron job")
                .register(meterRegistry);
        logger.info("Starting cron job scheduler");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
        logger.info("Registering cron jobs");
        cronJobsService.getActiveCronJobs().forEach(this::doScheduleJob);
        refreshNextExecutionGauge();
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
        doScheduleJob(cronJob);
        refreshNextExecutionGauge();
    }

    private void doScheduleJob(CronJob cronJob) {
        cancel(cronJob);

        if (cronJob.isActive()) {
            logger.info("Scheduling cron job " + cronJob.getId() + " - " + cronJob.getName() + " with " + cronJob.getCronExpression());
            ScheduledTask scheduledTask = taskRegistrar.scheduleCronTask(new CronTask(
                    () -> cronJobsService.execute(cronJob)
                    , cronJob.getCronExpression()
            ));
            tasksCache.add(cronJob.getId(), scheduledTask);
        }
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

    /**
     * Rebuilds the "next execution" gauge from the current set of active
     * jobs. {@link MultiGauge} has no per-row upsert, so any scheduling
     * change (job created/updated/deleted, reload) recomputes the whole set
     * — cheap enough since it only runs on admin actions, not on every tick.
     */
    private void refreshNextExecutionGauge() {
        var rows = cronJobsService.getActiveCronJobs().stream()
                .map(cj -> MultiGauge.Row.of(
                        Tags.of(
                                "job", cj.getName() != null ? cj.getName() : String.valueOf(cj.getId()),
                                "project", cj.getProject() != null ? cj.getProject().getName() : "unknown"
                        ),
                        toEpochSeconds(cj)
                ))
                .toList();
        nextExecutionGauge.register(rows, true);
    }

    private static double toEpochSeconds(CronJob cronJob) {
        var next = cronJob.getNextExecutionDateTime();
        return next != null ? next.atZone(ZoneId.systemDefault()).toEpochSecond() : Double.NaN;
    }
}
