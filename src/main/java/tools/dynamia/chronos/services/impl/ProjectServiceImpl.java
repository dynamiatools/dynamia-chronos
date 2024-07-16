package tools.dynamia.chronos.services.impl;

import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.notificators.NotificationSender;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.SimpleCache;
import tools.dynamia.domain.query.QueryParameters;
import tools.dynamia.domain.services.AbstractService;
import tools.dynamia.integration.Containers;
import tools.dynamia.integration.sterotypes.Service;
import tools.dynamia.modules.security.domain.User;

import java.util.List;

@Service
public class ProjectServiceImpl extends AbstractService implements ProjectService {

    private SimpleCache<String, Project> cache = new SimpleCache<>();
    private SimpleCache<String, List<Variable>> variablesCache = new SimpleCache<>();
    private SimpleCache<String, List<Notificator>> notificatorsCache = new SimpleCache<>();


    @Override
    public void clearCache(Project project) {
        cache.remove(project.getId());
        variablesCache.remove(project.getId());
        notificatorsCache.remove(project.getId());
    }

    @Override
    public Project getById(String id) {
        return cache.getOrLoad(id, key -> crudService().find(Project.class, id));
    }

    @Override
    public List<Variable> getVariables(Project project) {
        return variablesCache.getOrLoad(project.getId(), key -> crudService().find(Variable.class, "project", project));
    }

    @Override
    public List<Variable> getVariablesFor(CronJob cronJob) {
        return getVariables(cronJob.getProject());
    }

    @Override
    public List<CronJob> getCronJobs(Project project) {
        return crudService().find(CronJob.class, QueryParameters.with("active", true)
                .add("project", project));
    }

    @Override
    public List<Notificator> getNotificators(Project project) {
        return notificatorsCache.getOrLoad(project.getId(), key -> crudService().find(Notificator.class, "project", project));
    }

    @Override
    public NotificationSender findNotificationSender(String id) {
        return Containers.get().findObject(NotificationSender.class, obj -> obj.getId().equals(id));
    }

    @Override
    public List<Project> findAll() {
        return crudService().findAll(Project.class);
    }

    @Override
    public List<Project> findUserProjects(User user) {
        return crudService().getPropertyValues(ProjectRole.class, "project", QueryParameters.with("user", user));
    }

    @Override
    public List<RequestCollection> getCollections(Project project) {
        return crudService().find(RequestCollection.class, QueryParameters.with("project", project));
    }

    @Override
    public List<RequestCollection> getCollections(RequestCollection collection) {
        return crudService().find(RequestCollection.class, QueryParameters.with("parentCollection", collection));
    }

    @Override
    public List<RequestItem> getItems(RequestCollection collection) {
        return crudService().find(RequestItem.class, QueryParameters.with("collection", collection));
    }


}
