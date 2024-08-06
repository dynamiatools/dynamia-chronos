package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.Init;
import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.integration.Containers;
import tools.dynamia.modules.security.CurrentUser;
import tools.dynamia.zk.crud.ui.EntityTreeModel;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.crud.ui.LazyEntityTreeNode;
import tools.dynamia.zk.crud.ui.RootTreeNode;
import tools.dynamia.zk.util.ZKBindingUtil;

import java.util.List;

public abstract class AbstractProjectsViewModel {


    protected final ProjectService projectService = Containers.get().findObject(ProjectService.class);
    protected final CronJobsService cronJobsService = Containers.get().findObject(CronJobsService.class);

    protected List<Project> projects;
    protected List<ProjectRole> roles;

    protected EntityTreeModel<? extends SimpleEntity> treeModel;
    private EntityTreeNode<SimpleEntity> selectedNode;


    @Init
    public void init() {
        projects = projectService.findUserProjects(CurrentUser.get().getUser());
        roles = projectService.findProjectRoles(CurrentUser.get().getUser());
        loadModel();
    }

    protected UserRole getRole(Project project) {
        return roles.stream().filter(r -> r.getProject().equals(project)).map(ProjectRole::getRole).findFirst().orElse(null);
    }


    protected void loadModel() {
        RootTreeNode<SimpleEntity> root = new RootTreeNode<>(new Project("Projects"));

        projects.forEach(project -> {
            var cronJobsNode = loadCronJobNode(project);
            var collectionsNode = loadCollectionsNode(project);

            var projectNode = buildNode(project);
            root.addChild(projectNode);
            if (cronJobsNode != null) {
                cronJobsNode.setContextMenuID("cronjobs-menu");
                projectNode.addChild(cronJobsNode);
            }
            if (collectionsNode != null) {
                collectionsNode.setContextMenuID("collections-menu");
                projectNode.addChild(collectionsNode);
            }

            loadMoreNodes(project, projectNode);
        });


        treeModel = new EntityTreeModel<>(root);

    }


    protected LazyEntityTreeNode<SimpleEntity> loadCollectionsNode(Project project) {
        var collectionsNode = new LazyEntityTreeNode<SimpleEntity>("Requests", "folder");
        collectionsNode.setEntity(new RequestCollection(project));
        collectionsNode.setContextMenuID("collections-menu");
        collectionsNode.setSource(project);
        collectionsNode.setLoader(node -> {
            var collections = projectService.getCollections(project);
            loadCollections(project, collections, node);
            notifyChanges();
        });
        return collectionsNode;
    }

    protected LazyEntityTreeNode<SimpleEntity> loadCronJobNode(Project project) {

        var cronJobsNode = new LazyEntityTreeNode<SimpleEntity>("Cron jobs", "fa-tasks");
        cronJobsNode.setEntity(new CronJob(project));
        cronJobsNode.setLoader(node -> {
            var cronjobs = projectService.getCronJobs(project);
            cronjobs.forEach(cronJob -> {
                var childNode = buildNode(cronJob, project);
                node.addChild(childNode);
            });
            notifyChanges();
        });
        return cronJobsNode;
    }


    private void loadCollections(Project project, List<RequestCollection> collections, LazyEntityTreeNode<SimpleEntity> parentNode) {

        collections.forEach(collection -> {
            var collectionNode = buildNode(collection, project);
            parentNode.addChild(collectionNode);
        });
    }


    protected abstract void loadMoreNodes(Project project, EntityTreeNode<SimpleEntity> projectNode);

    public abstract void nodeSelected();


    protected void notifyChanges() {
        ZKBindingUtil.postNotifyChange(this);
    }

    public EntityTreeModel<? extends SimpleEntity> getTreeModel() {
        return treeModel;
    }

    public List<Project> getProjects() {
        return projects;
    }


    public void setSelectedNode(EntityTreeNode<SimpleEntity> selectedNode) {
        this.selectedNode = selectedNode;
    }

    public EntityTreeNode<SimpleEntity> getSelectedNode() {
        return selectedNode;
    }

    protected LazyEntityTreeNode<SimpleEntity> buildNode(RequestCollection collection, Project project) {
        var role = getRole(project);
        var collectionNode = new LazyEntityTreeNode<SimpleEntity>(collection);
        collectionNode.setLabel(collection.getTitle());
        collectionNode.setIcon("folder");
        collectionNode.setRole(role);
        collectionNode.setSource(project);
        collectionNode.setContextMenuID("collection-menu");
        collectionNode.setLoader(node -> {
            List<RequestCollection> subcollections = projectService.getCollections(collection);
            if (!subcollections.isEmpty()) {
                loadCollections(project, subcollections, node);
            }

            List<RequestItem> items = projectService.getItems(collection);
            items.forEach(item -> {
                var itemNode = buildNode(item, project);
                node.addChild(itemNode);
            });

            notifyChanges();
        });
        return collectionNode;
    }

    protected EntityTreeNode<SimpleEntity> buildNode(Project project) {
        var projectNode = new EntityTreeNode<SimpleEntity>(project);
        projectNode.setRole(getRole(project));
        projectNode.setIcon("fa-clock");
        projectNode.setContextMenuID("project-menu");
        projectNode.setSource(project);
        return projectNode;
    }

    protected EntityTreeNode<SimpleEntity> buildNode(CronJob cronJob, Project project) {
        var childNode = new EntityTreeNode<SimpleEntity>(cronJob);
        childNode.setRole(getRole(project));
        childNode.setIcon("fa-cog");
        childNode.setBadge(cronJob.getStatus());
        childNode.setTooltiptext(cronJob.getServerHost());
        childNode.setSource(project);
        childNode.setContextMenuID("cronjob-menu");
        return childNode;
    }

    protected EntityTreeNode<SimpleEntity> buildNode(RequestItem item, Project project) {
        var itemNode = new EntityTreeNode<SimpleEntity>(item);
        itemNode.setLabel(item.getName());
        itemNode.setBadge(item.getHttpMethod().name());
        itemNode.setBadgePosition("left");
        itemNode.setTooltiptext(item.getServerHost());
        //itemNode.setIcon("fa-angle-right");
        itemNode.setRole(getRole(project));
        itemNode.setSource(project);

        return itemNode;
    }
}
