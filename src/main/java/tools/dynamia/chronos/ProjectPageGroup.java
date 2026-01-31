package tools.dynamia.chronos;

import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.integration.Containers;
import tools.dynamia.modules.security.CurrentUser;
import tools.dynamia.navigation.Page;
import tools.dynamia.navigation.PageGroup;

import java.util.List;

public class ProjectPageGroup extends PageGroup {


    public ProjectPageGroup() {
        super("projects", "Projects");
        setIcon("fa-tasks");
    }

    @Override
    public List<Page> getPages() {
        ProjectService service = Containers.get().findObject(ProjectService.class);
        if (CurrentUser.get() != null) {
            var projects = service.findUserProjects(CurrentUser.get().getUser());
            return projects.stream().map(this::projectPage).toList();
        } else {
            return List.of();
        }
    }

    private Page projectPage(Project project) {
        var page = new Page();
        page.setAlwaysAllowed(true);
        page.addAttribute("project", project);
        page.setId("project_" + project.getId());
        page.setName(project.getName());
        page.setPath("classpath:/zk/pages/projects.zul");
        page.setIcon("fa-check-circle");
        page.setPageGroup(this);
        return page;
    }
}
