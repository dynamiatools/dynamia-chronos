package tools.dynamia.chronos.services;

import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.notificators.NotificationSender;
import tools.dynamia.modules.security.domain.User;

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

    List<Project> findUserProjects(User user);

    List<RequestCollection> getCollections(Project project);

    List<RequestCollection> getCollections(RequestCollection collection);

    List<RequestItem> getItems(RequestCollection collection);

    List<ProjectRole> findProjectRoles(User user);

    RequestCollection importCollectionFromPostman(String postmanJson);

    List<Variable> getVariables(RequestCollection collection);

    ProjectRole findProjectRole(Project project, User user);
}
