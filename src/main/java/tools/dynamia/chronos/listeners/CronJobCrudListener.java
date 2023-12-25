package tools.dynamia.chronos.listeners;

import tools.dynamia.chronos.CronJobScheduler;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.domain.util.CrudServiceListenerAdapter;
import tools.dynamia.integration.sterotypes.Listener;

@Listener
public class CronJobCrudListener extends CrudServiceListenerAdapter<CronJob> {

    private final CronJobScheduler scheduler;

    public CronJobCrudListener(CronJobScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private void updateCronJob(CronJob cronJob) {
        scheduler.scheduleJob(cronJob);
    }

    @Override
    public void afterCreate(CronJob entity) {
        updateCronJob(entity);
    }

    @Override
    public void afterUpdate(CronJob entity) {
        updateCronJob(entity);
    }

    @Override
    public void afterDelete(CronJob entity) {
        updateCronJob(entity);
    }
}
