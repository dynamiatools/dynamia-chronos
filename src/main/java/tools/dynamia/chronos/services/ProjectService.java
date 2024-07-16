package tools.dynamia.chronos.services;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.chronos.notificators.NotificationSender;

import java.util.List;

/**
 * Project service for cron jobs
 */
public interface ProjectService {

    Project getById(String id);

    void clearCache(Project project);

    List<Variable> getVariables(Project project);

    List<Variable> getVariablesFor(CronJob cronJob);


    List<CronJob> getCronJobs(Project project);

    List<Notificator> getNotificators(Project project);

    NotificationSender findNotificationSender(String id);


    List<Project> findAll();
}
