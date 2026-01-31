package tools.dynamia.chronos.actions;

import org.zkoss.util.resource.Labels;
import org.zkoss.zul.Messagebox;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.commons.Callback;
import tools.dynamia.crud.AbstractCrudAction;
import tools.dynamia.crud.CrudActionEvent;
import tools.dynamia.domain.jpa.ContactInfo;
import tools.dynamia.integration.Containers;
import tools.dynamia.ui.UIMessages;

import java.util.function.Consumer;

@InstallAction
public class TestCronJobAction extends AbstractCrudAction {

    private final CronJobsService cronJobsService;

    public TestCronJobAction(CronJobsService cronJobsService) {
        this.cronJobsService = cronJobsService;
        setName("Test");
        setMenuSupported(true);
        setApplicableClass(CronJob.class);
    }

    public static TestCronJobAction get() {
        return Containers.get().findObject(TestCronJobAction.class);
    }

    @Override
    public void actionPerformed(CrudActionEvent evt) {
        if (evt.getData() instanceof CronJob cronJob) {
            test(cronJob, log -> evt.getController().doQuery());
        }
    }

    public void test(CronJob cronJob, Consumer<CronJobLog> onTestFinish) {
        UIMessages.showQuestion(Labels.getLabel("areYouSure"), () -> {
            var log = cronJobsService.test(cronJob);
            Messagebox.show(log.toHtml());
            if (onTestFinish != null) {
                onTestFinish.accept(log);
            }
        });
    }
}
