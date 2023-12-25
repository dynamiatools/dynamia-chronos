package tools.dynamia.chronos.services.impl;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.chronos.notificators.NotificationSender;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.SimpleCache;
import tools.dynamia.domain.services.AbstractService;
import tools.dynamia.integration.Containers;
import tools.dynamia.integration.sterotypes.Service;

import java.util.List;

@Service
public class ProjectServiceImpl extends AbstractService implements ProjectService {

    private SimpleCache<Long, Project> cache = new SimpleCache<>();
    private SimpleCache<Long, List<Variable>> variablesCache = new SimpleCache<>();
    private SimpleCache<Long, List<Notificator>> notificatorsCache = new SimpleCache<>();


    @Override
    public void clearCache(Project project) {
        cache.remove(project.getId());
        variablesCache.remove(project.getId());
        notificatorsCache.remove(project.getId());
    }

    @Override
    public Project getById(Long id) {
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
        return crudService().find(CronJob.class, "project", project);
    }

    @Override
    public List<Notificator> getNotificators(Project project) {
        return notificatorsCache.getOrLoad(project.getId(), key -> crudService().find(Notificator.class, "project", project));
    }

    @Override
    public NotificationSender findNotificationSender(String id) {
        return Containers.get().findObject(NotificationSender.class, obj -> obj.getId().equals(id));
    }

}
