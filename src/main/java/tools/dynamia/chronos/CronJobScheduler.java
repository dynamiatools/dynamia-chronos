package tools.dynamia.chronos;

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

/*
Main schedule configurer
 */
@Service
public class CronJobScheduler implements SchedulingConfigurer {

    private SimpleCache<Long, ScheduledTask> tasksCache = new SimpleCache<>();
    private LoggingService logger = new SLF4JLoggingService(CronJobScheduler.class);

    private final CronJobsService cronJobsService;
    private ScheduledTaskRegistrar taskRegistrar;

    public CronJobScheduler(CronJobsService cronJobsService) {
        this.cronJobsService = cronJobsService;
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
}
