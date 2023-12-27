package tools.dynamia.chronos.services.impl;

import tools.dynamia.chronos.CronJobExecutor;
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

    private static final String FAILING_STATUS = "Failing";
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
        Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.beforeExecution(cronJob));

        var executor = new CronJobExecutor(cronJob, projectService.getVariablesFor(cronJob),
                message -> log("[JOB-" + cronJob.getId() + "] " + message));

        var log = executor.execute();

        crudService().executeWithinTransaction(() -> {
            crudService().increaseCounter(cronJob, "executionsCount");
            crudService().batchUpdate(CronJob.class,
                    MapBuilder.put(
                            "lastExecution", LocalDateTime.now(),
                            "status", log.isFail() ? FAILING_STATUS : "OK"
                    ),
                    QueryParameters.with("id", cronJob.getId()));
            crudService().create(log);
        });

        Containers.get().findObjects(CronJobExecutionListener.class).forEach(l -> l.afterExecution(cronJob, log));
    }

    @Override
    public CronJobLog test(CronJob cronJob) {
        var executor = new CronJobExecutor(cronJob, projectService.getVariablesFor(cronJob),
                message -> log("[TEST JOB-" + cronJob.getId() + "] " + message));
        return executor.execute();
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
    public long getFailingCronJobsCount() {
        return crudService().count(CronJob.class, QueryParameters.with("active", true)
                .add("status", FAILING_STATUS));
    }
}
