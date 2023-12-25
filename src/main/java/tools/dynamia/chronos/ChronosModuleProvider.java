package tools.dynamia.chronos;

import tools.dynamia.chronos.domain.Project;
import tools.dynamia.crud.CrudPage;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.navigation.Module;
import tools.dynamia.navigation.ModuleProvider;

@Provider
public class ChronosModuleProvider implements ModuleProvider {
    @Override
    public Module getModule() {
        var mod = new Module("main", "Cnronos");
        mod.setIcon("fa-clock");

        mod.addPage(new CrudPage("projects", "Projects", Project.class)
                .featured()
                .icon("fa-tasks"));
        return mod;
    }
}
