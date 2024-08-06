package tools.dynamia.chronos.services.impl;

import tools.dynamia.chronos.ChronosHttpMethod;
import tools.dynamia.chronos.domain.*;
import tools.dynamia.chronos.notificators.NotificationSender;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.SimpleCache;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.domain.query.QueryParameters;
import tools.dynamia.domain.services.AbstractService;
import tools.dynamia.integration.Containers;
import tools.dynamia.integration.sterotypes.Service;
import tools.dynamia.modules.security.domain.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return crudService().find(CronJob.class, QueryParameters.with("active", true)
                .add("project", project));
    }

    @Override
    public List<Notificator> getNotificators(Project project) {
        return notificatorsCache.getOrLoad(project.getId(), key -> crudService().find(Notificator.class, "project", project));
    }

    @Override
    public NotificationSender findNotificationSender(Long id) {
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

    @Override
    public List<ProjectRole> findProjectRoles(User user) {
        return crudService().find(ProjectRole.class, QueryParameters.with("user", user));
    }


    @Override
    public RequestCollection importCollectionFromPostman(String postmanJson) {
        Map<String, Object> json = StringPojoParser.parseJsonToMap(postmanJson);
        RequestCollection result = new RequestCollection();
        if (json.get("info") instanceof Map info) {
            result.setTitle((String) info.get("name"));
        }

        if (json.get("variable") instanceof List vars) {
            vars.forEach(obj -> {
                if (obj instanceof Map v) {
                    var variable = new Variable(String.valueOf(v.get("key")), String.valueOf(v.get("value")));
                    result.getVariables().add(variable);
                    variable.setCollection(result);
                }
            });


        }
        if (json.get("item") instanceof List items) {
            items.forEach(it -> {
                if (it instanceof Map<?, ?> subitem) {
                    importCollection(subitem, result);
                }
            });
            System.out.println(json);
        }
        return result;

    }

    private static void importCollection(Map<?, ?> subitem, RequestCollection parent) {
        if (subitem.get("item") instanceof List items) {
            RequestCollection subCollection = new RequestCollection();
            subCollection.setTitle((String) subitem.get("name"));
            subCollection.setParentCollection(parent);
            parent.getCollections().add(subCollection);
            subCollection.setParentCollection(parent);

            items.forEach(it -> {
                if (it instanceof Map<?, ?> subsubitem) {
                    importCollection(subsubitem, subCollection);
                }
            });

        } else if (subitem.get("request") instanceof Map request) {
            RequestItem item = new RequestItem();
            item.setCollection(parent);
            item.setDescription((String) request.get("description"));
            item.setName((String) subitem.get("name"));
            item.setHttpMethod(ChronosHttpMethod.valueOf((String) request.get("method")));
            if (request.get("url") instanceof Map url) {
                item.setServerHost((String) url.get("raw"));
                if (url.get("variable") instanceof List params) {
                    var paramsMap = new HashMap<String, String>();
                    params.forEach(p -> {
                        if (p instanceof Map<?, ?> param) {
                            paramsMap.put((String) param.get("key"), (String) param.get("value"));
                        }
                    });
                    if (!paramsMap.isEmpty()) {
                        item.setParameters(paramsMap);
                    }
                }

            }
            if (request.get("body") instanceof Map body) {
                item.setRequestBody((String) body.get("raw"));
            }
            if (request.get("header") instanceof List headers) {
                Map<String, String> itemsHeader = new HashMap<>();
                headers.forEach(h -> {
                    if (h instanceof Map<?, ?> header) {
                        itemsHeader.put((String) header.get("key"), (String) header.get("value"));
                    }
                });
                item.setHeaders(itemsHeader);
            }
            parent.getRequests().add(item);
            item.setCollection(parent);
        }
    }

    @Override
    public List<Variable> getVariables(RequestCollection collection) {
        return crudService().find(Variable.class, "collection", collection);
    }


    @Override
    public ProjectRole findProjectRole(Project project, User user) {
        return crudService().findSingle(ProjectRole.class, QueryParameters.with("project", project).add("user", user));
    }
}
