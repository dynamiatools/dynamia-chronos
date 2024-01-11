package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import tools.dynamia.actions.FastAction;
import tools.dynamia.chronos.ProjectAware;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.domain.util.DomainUtils;
import tools.dynamia.integration.Containers;
import tools.dynamia.zk.crud.CrudView;
import tools.dynamia.zk.crud.ui.EntityTreeModel;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.crud.ui.LazyEntityTreeNode;
import tools.dynamia.zk.crud.ui.RootTreeNode;
import tools.dynamia.zk.util.ZKBindingUtil;
import tools.dynamia.zk.viewers.ui.Viewer;

import java.util.List;

public class DashboardViewModel {


    private ProjectService projectService = Containers.get().findObject(ProjectService.class);
    private CronJobsService cronJobsService = Containers.get().findObject(CronJobsService.class);


    private List<Project> projects;

    private long projectsCount;
    private long activeCronJobsCount;
    private long failingCronJobsCount;

    private long okCronJobsCount;

    private EntityTreeModel<? extends SimpleEntity> treeModel;

    private EntityTreeNode selectedNode;
    private String viewerType;
    private String viewerClass;
    private Object viewerValue;
    private Component view;

    @Init
    public void init() {
        projects = projectService.findAll();
        projectsCount = projects.size();
        activeCronJobsCount = cronJobsService.getActiveCronJobsCount();
        failingCronJobsCount = cronJobsService.countCronJobsByStatus(CronJobsService.FAILING_STATUS);
        okCronJobsCount = cronJobsService.countCronJobsByStatus(CronJobsService.OK_STATUS);
        loadModel();
    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        this.view = view;
    }

    private void loadModel() {
        RootTreeNode<SimpleEntity> root = new RootTreeNode<>(new Project("Projects"));

        projects.forEach(project -> {

            var variablesNode = new LazyEntityTreeNode<SimpleEntity>("Variables", "fa-hashtag");
            variablesNode.setEntity(new Variable());

            var notificatorsNode = new LazyEntityTreeNode<SimpleEntity>("Notificators", "fa-bell");
            notificatorsNode.setEntity(new Notificator());

            var cronJobsNode = new LazyEntityTreeNode<SimpleEntity>("Cron jobs", "fa-tasks");
            cronJobsNode.setEntity(new CronJob());

            var projectNode = root.addChild(project);
            projectNode.setIcon("fa-clock");
            projectNode.addChild(cronJobsNode);
            projectNode.addChild(variablesNode);
            projectNode.addChild(notificatorsNode);


            cronJobsNode.setLoader(node -> {
                var cronjobs = projectService.getCronJobs(project);
                cronjobs.forEach(cronJob -> {
                    var childNode = node.addChild(cronJob);
                    childNode.setIcon("fa-cog");
                    childNode.setBadge(cronJob.getStatus());
                    childNode.setTooltiptext(cronJob.getServerHost());
                });
                if (cronjobs.isEmpty()) {
                    node.addChild(new EntityTreeNode<>("no active cron jobs", ""));
                }
                notifyChanges();
            });

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

            variablesNode.setLoader(node -> {
                var variables = projectService.getVariables(project);
                node.addChildren(variables, "fa-hashtag");
                if (variables.isEmpty()) {
                    node.addChild(new EntityTreeNode<>("empty", ""));
                }
                notifyChanges();
            });
        });


        treeModel = new EntityTreeModel<>(root);


    }

    @Command
    public void nodeSelected() {
        if (selectedNode != null) {
            viewerValue = null;
            var entity = selectedNode.getEntity();
            if (entity instanceof CronJob cronJob && cronJob.getId() != null) {
                viewerType = "table";
                viewerClass = CronJobLog.class.getName();
                viewerValue = cronJobsService.getLatestLogs(cronJob);

            } else if (entity instanceof SimpleEntity value && value.getId() != null) {
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


                viewer.addAction(new FastAction("Edit ", evt -> {
                    CrudView.showUpdateView("Edit - " + entity, entity.getClass(), DomainUtils.lookupCrudService().reload(entity), () -> {
                        if (entity instanceof ProjectAware pa) {
                            projectService.clearCache(pa.getProject());
                        }
                        init();
                        notifyChanges();
                    });
                }));


                var content = (Center) view.query("center");
                content.getChildren().clear();
                content.setTitle(selectedNode.getParent().getLabel());
                if (entity instanceof CronJob cron && cron.getId() != null) {
                    content.setTitle("Cron Job: " + cron.getServerHost()+" - Next Execution: "+cron.getNextExecution());
                }
                content.appendChild(viewer);
            }

        }

        notifyChanges();
    }

    private void notifyChanges() {
        ZKBindingUtil.postNotifyChange(this);
    }

    public EntityTreeModel<? extends SimpleEntity> getTreeModel() {
        return treeModel;
    }

    public List<Project> getProjects() {
        return projects;
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

    public void setSelectedNode(EntityTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public EntityTreeNode getSelectedNode() {
        return selectedNode;
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
