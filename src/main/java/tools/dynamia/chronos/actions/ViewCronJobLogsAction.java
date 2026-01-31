package tools.dynamia.chronos.actions;

import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.crud.AbstractCrudAction;
import tools.dynamia.crud.CrudActionEvent;
import tools.dynamia.integration.Containers;
import tools.dynamia.zk.viewers.ui.Viewer;

@InstallAction
public class ViewCronJobLogsAction extends AbstractCrudAction {

    public static ViewCronJobLogsAction get() {
        return Containers.get().findObject(ViewCronJobLogsAction.class);
    }

    private final CronJobsService cronJobsService;

    public ViewCronJobLogsAction(CronJobsService cronJobsService) {
        this.cronJobsService = cronJobsService;
        setName("Logs");
        setMenuSupported(true);
        setApplicableClass(CronJob.class);
    }

    @Override
    public void actionPerformed(CrudActionEvent evt) {
        if (evt.getData() instanceof CronJob cronJob) {
            viewLogs(cronJob);
        }
    }

    public void viewLogs(CronJob cronJob) {
        var logs = cronJobsService.getLatestLogs(cronJob);
        var dialog = Viewer.showDialog("Logs - " + cronJob, "table", CronJobLog.class, logs);
        dialog.setHeight("90%");
        dialog.setWidth("90%");
    }
}
