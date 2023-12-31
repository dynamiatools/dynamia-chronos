package tools.dynamia.chronos.services;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;

import java.util.List;

/**
 * Cron jobs service
 */
public interface CronJobsService {

    String FAILING_STATUS = "Failing";
    String OK_STATUS = "OK";

    /**
     * Find all active cron jobs from all projects
     *
     * @return cron jobs list
     */
    List<CronJob> getActiveCronJobs();

    List<CronJob> getFailingCronJobs();

    void execute(CronJob cronJob);

    CronJobLog test(CronJob cronJob);

    /**
     * Return lastes logs
     *
     * @param cronJob
     * @return
     */
    List<CronJobLog> getLatestLogs(CronJob cronJob);

    long getActiveCronJobsCount();

    long countCronJobsByStatus(String status);
}
