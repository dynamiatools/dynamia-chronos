package tools.dynamia.chronos.actions;

import org.zkoss.zul.Messagebox;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.crud.AbstractCrudAction;
import tools.dynamia.crud.CrudActionEvent;
import tools.dynamia.web.util.HtmlTableBuilder;

@InstallAction
public class TestCronJobAction extends AbstractCrudAction {

    private final CronJobsService cronJobsService;

    public TestCronJobAction(CronJobsService cronJobsService) {
        this.cronJobsService = cronJobsService;
        setName("Test");
        setMenuSupported(true);
        setApplicableClass(CronJob.class);
    }

    @Override
    public void actionPerformed(CrudActionEvent evt) {
        if (evt.getData() instanceof CronJob cronJob) {
            var log = cronJobsService.test(cronJob);



            Messagebox.show(log.toHtml());

        }
    }
}
