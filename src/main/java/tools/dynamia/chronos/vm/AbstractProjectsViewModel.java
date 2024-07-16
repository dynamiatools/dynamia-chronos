package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;
import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.services.CronJobsService;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
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

    protected EntityTreeModel<? extends SimpleEntityUuid> treeModel;
    private EntityTreeNode<SimpleEntityUuid> selectedNode;


    @Init
    public void init() {
        projects = projectService.findUserProjects(CurrentUser.get().getUser());
        loadModel();
    }

    protected void loadModel() {
        RootTreeNode<SimpleEntityUuid> root = new RootTreeNode<>(new Project("Projects"));

        projects.forEach(project -> {

            var cronJobsNode = loadCronJobNode(project);
            var collectionsNode = loadCollectionsNode(project);

            var projectNode = root.addChild(project);
            projectNode.setIcon("fa-clock");
            if (cronJobsNode != null) {
                projectNode.addChild(cronJobsNode);
            }
            if (collectionsNode != null) {
                projectNode.addChild(collectionsNode);
            }

            loadMoreNodes(project, projectNode);
        });


        treeModel = new EntityTreeModel<>(root);

    }

    protected LazyEntityTreeNode<SimpleEntityUuid> loadCollectionsNode(Project project) {
        var collectionsNode = new LazyEntityTreeNode<SimpleEntityUuid>("Requests", "folder");
        collectionsNode.setEntity(new RequestCollection());
        collectionsNode.setLoader(node -> {
            var collections = projectService.getCollections(project);
            loadCollections(collections, node);
            notifyChanges();
        });
        return collectionsNode;
    }

    protected LazyEntityTreeNode<SimpleEntityUuid> loadCronJobNode(Project project) {
        var cronJobsNode = new LazyEntityTreeNode<SimpleEntityUuid>("Cron jobs", "fa-tasks");
        cronJobsNode.setEntity(new CronJob());
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
        return cronJobsNode;
    }


    private void loadCollections(List<RequestCollection> collections, LazyEntityTreeNode<SimpleEntityUuid> parentNode) {

        collections.forEach(collection -> {
            var collectionNode = new LazyEntityTreeNode<SimpleEntityUuid>(collection);
            collectionNode.setLabel(collection.getTitle());
            collectionNode.setIcon("folder");
            collectionNode.setLoader(node -> {
                List<RequestCollection> subcollections = projectService.getCollections(collection);
                if (!subcollections.isEmpty()) {
                    loadCollections(subcollections, node);
                }

                List<RequestItem> items = projectService.getItems(collection);
                items.forEach(item -> {
                    var itemNode = new EntityTreeNode<SimpleEntityUuid>(item);
                    itemNode.setLabel(item.getName());
                    itemNode.setBadge(item.getHttpMethod().name());
                    itemNode.setTooltiptext(item.getServerHost());
                    itemNode.setIcon("fa-angle-right");
                    node.addChild(itemNode);
                });

                notifyChanges();
            });

            parentNode.addChild(collectionNode);

        });

        if (collections.isEmpty()) {
            parentNode.addChild(new EntityTreeNode<>("empty collections", ""));
        }
    }

    protected abstract void loadMoreNodes(Project project, EntityTreeNode<SimpleEntityUuid> projectNode);

    public abstract void nodeSelected();


    protected void notifyChanges() {
        ZKBindingUtil.postNotifyChange(this);
    }

    public EntityTreeModel<? extends SimpleEntityUuid> getTreeModel() {
        return treeModel;
    }

    public List<Project> getProjects() {
        return projects;
    }


    public void setSelectedNode(EntityTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public EntityTreeNode getSelectedNode() {
        return selectedNode;
    }


}
