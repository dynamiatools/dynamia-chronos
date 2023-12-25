package tools.dynamia.chronos.listeners;

import tools.dynamia.chronos.CronJobScheduler;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.domain.util.CrudServiceListenerAdapter;
import tools.dynamia.integration.sterotypes.Listener;

@Listener
public class ProjectCrudListener extends CrudServiceListenerAdapter<Project> {

    private final ProjectService projectService;
    private final CronJobScheduler scheduler;

    public ProjectCrudListener(ProjectService projectService, CronJobScheduler scheduler) {
        this.projectService = projectService;
        this.scheduler = scheduler;
    }

    @Override
    public void afterUpdate(Project project) {
        projectService.clearCache(project);
        projectService.getCronJobs(project).forEach(scheduler::scheduleJob);

    }
}
