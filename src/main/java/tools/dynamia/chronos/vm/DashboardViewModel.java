package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import tools.dynamia.actions.FastAction;
import tools.dynamia.chronos.ProjectAware;
import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.domain.AbstractEntity;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.domain.util.DomainUtils;
import tools.dynamia.zk.crud.CrudView;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.crud.ui.LazyEntityTreeNode;
import tools.dynamia.zk.viewers.ui.Viewer;

public class DashboardViewModel extends AbstractProjectsViewModel {


    private long projectsCount;
    private long activeCronJobsCount;
    private long failingCronJobsCount;

    private long okCronJobsCount;


    private String viewerType;
    private String viewerClass;
    private Object viewerValue;
    private Component view;

    @Init
    public void init() {
        super.init();
        projectsCount = projects.size();
        activeCronJobsCount = cronJobsService.getActiveCronJobsCount();
        failingCronJobsCount = cronJobsService.countCronJobsByStatus(CronJobsService.FAILING_STATUS);
        okCronJobsCount = cronJobsService.countCronJobsByStatus(CronJobsService.OK_STATUS);

    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        this.view = view;
    }

    @Override
    protected void loadMoreNodes(Project project, EntityTreeNode<SimpleEntityUuid> projectNode) {
        loadVariablesNode(project, projectNode);
        loadNotificators(project, projectNode);

    }

    private void loadNotificators(Project project, EntityTreeNode<SimpleEntityUuid> projectNode) {
        var notificatorsNode = new LazyEntityTreeNode<SimpleEntityUuid>("Notificators", "fa-bell");
        notificatorsNode.setEntity(new Notificator());
        notificatorsNode.setLoader(node -> {
            var notificators = projectService.getNotificators(project);
            notificators.forEach(nf -> {
                var nofNode = node.addChild(nf);
                var sender = projectService.findNotificationSender(nf.getSender());
                nofNode.setIcon(sender.getIcon());
                nofNode.setLabel(nf.getContact());
                if (nofNode.getLabel().length() > 60) {
                    nofNode.setLabel(sender.getName());
                }
            });
            if (notificators.isEmpty()) {
                node.addChild(new EntityTreeNode<>("empty", ""));
            }
            notifyChanges();
        });
        projectNode.addChild(notificatorsNode);
    }

    private void loadVariablesNode(Project project, EntityTreeNode<SimpleEntityUuid> projectNode) {
        var variablesNode = new LazyEntityTreeNode<SimpleEntityUuid>("Variables", "fa-hashtag");
        variablesNode.setEntity(new Variable());
        variablesNode.setLoader(node -> {
            var variables = projectService.getVariables(project);
            node.addChildren(variables, "fa-hashtag");
            if (variables.isEmpty()) {
                node.addChild(new EntityTreeNode<>("empty", ""));
            }
            notifyChanges();
        });
        projectNode.addChild(variablesNode);
    }


    @Command
    public void nodeSelected() {
        if (getSelectedNode() != null) {
            viewerValue = null;
            var entity = getSelectedNode().getEntity();
            if (entity instanceof CronJob cronJob && cronJob.getId() != null) {
                viewerType = "table";
                viewerClass = CronJobLog.class.getName();
                viewerValue = cronJobsService.getLatestLogs(cronJob);

            } else if (entity instanceof AbstractEntity value && value.getId() != null) {
                viewerType = "form";
                viewerClass = value.getClass().getName();
                viewerValue = value;
            }

            if (viewerValue != null) {
                Viewer viewer = new Viewer();
                viewer.setViewType(viewerType);
                viewer.setBeanClass(viewerClass);
                viewer.setValue(viewerValue);
                viewer.setVflex("1");
                viewer.setReadonly(true);



                var content = (Center) view.query("center");
                content.getChildren().clear();
                content.setTitle(getSelectedNode().getParent().getLabel());
                if (entity instanceof CronJob cron && cron.getId() != null) {
                    content.setTitle("Cron Job: " + cron.getServerHost() + " - Next Execution: " + cron.getNextExecution());
                }
                content.appendChild(viewer);
            }

        }

        notifyChanges();
    }


    public long getProjectsCount() {
        return projectsCount;
    }

    public long getActiveCronJobsCount() {
        return activeCronJobsCount;
    }

    public long getFailingCronJobsCount() {
        return failingCronJobsCount;
    }

    public String getViewerType() {
        return viewerType;
    }

    public String getViewerClass() {
        return viewerClass;
    }

    public Object getViewerValue() {
        return viewerValue;
    }

    public long getOkCronJobsCount() {
        return okCronJobsCount;
    }
}
