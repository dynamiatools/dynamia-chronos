package tools.dynamia.chronos;

import tools.dynamia.chronos.domain.Project;
import tools.dynamia.crud.CrudPage;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.navigation.Module;
import tools.dynamia.navigation.ModuleProvider;

@Provider
public class ChronosSystemModuleProvider implements ModuleProvider {

    @Override
    public Module getModule() {
        Module system = Module.getRef("system");

        system.addPage(new CrudPage("projectsDetails", "Projects Details", Project.class)
                .icon("fa-tasks"));
        return system;
    }


}
