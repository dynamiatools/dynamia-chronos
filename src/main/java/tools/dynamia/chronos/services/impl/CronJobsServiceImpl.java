package tools.dynamia.chronos.services.impl;

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
import java.util.List;
import java.util.Map;

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
        fireBeforeExecutionListener(cronJob);

        var prefix = testMode ? "[TEST] " : "[JOB] ";
        try {
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

            fireAfterExecutionListener(cronJob, log);
            return (CronJobLog) log;
        } catch (Exception e) {
            log(prefix + "Error executing cron job: " + e.getMessage());
            CronJobLog errorLog = new CronJobLog(cronJob, false, true);
            errorLog.setResponse(StringPojoParser.convertMapToJson(Map.of(
                    "exception", e.getClass().getSimpleName(),
                    "message", e.getMessage(),
                    "cause", e.getCause()))
            );
            errorLog.setTest(testMode);
            errorLog.setDetails("Error executing cron job: " + e.getMessage());
            errorLog.setStatus("ERROR");
            errorLog.setStatusCode(500);
            crudService().create(errorLog);
            fireAfterExecutionListener(cronJob, errorLog);
            return errorLog;
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
