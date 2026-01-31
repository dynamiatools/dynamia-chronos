package tools.dynamia.chronos.services.impl;

import tools.dynamia.chronos.CronJobHttpRequestExecutor;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.listeners.CronJobExecutionListener;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.MapBuilder;
import tools.dynamia.domain.query.QueryParameters;
import tools.dynamia.domain.services.AbstractService;
import tools.dynamia.integration.Containers;
import tools.dynamia.integration.sterotypes.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CronJobsServiceImpl extends AbstractService implements CronJobsService {


    private final ProjectService projectService;

    public CronJobsServiceImpl(ProjectService projectService) {
        this.projectService = projectService;
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
        Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.beforeExecution(cronJob));

        var prefix = testMode ? "[TEST] " : "[JOB] ";
        var executor = new CronJobHttpRequestExecutor(cronJob, projectService.getVariablesFor(cronJob),
                message -> log(prefix + message));

        var log = executor.execute();
        if (log instanceof CronJobLog cl) {
            cl.setTest(testMode);
        }

        crudService().executeWithinTransaction(() -> {
            crudService().increaseCounter(cronJob, "executionsCount");
            crudService().batchUpdate(CronJob.class,
                    MapBuilder.put(
                            "lastExecution", LocalDateTime.now(),
                            "status", log.isFail() ? FAILING_STATUS : OK_STATUS
                    ),
                    QueryParameters.with("id", cronJob.getId()));
            crudService().create(log);
        });

        Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.afterExecution(cronJob, (CronJobLog) log));

        return (CronJobLog) log;
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
