package tools.dynamia.chronos;

import tools.dynamia.chronos.domain.Project;
import tools.dynamia.crud.CrudPage;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.navigation.Module;
import tools.dynamia.navigation.ModuleProvider;
import tools.dynamia.navigation.Page;

@Provider
public class ChronosModuleProvider implements ModuleProvider {
    @Override
    public Module getModule() {
        var mod = new Module("main", "Chronos");
        mod.setIcon("fa-clock");

        mod.addPage(new Page("dashboard","Dashboard","classpath:/zk/pages/dashboard.zul")
                .featured()
                .icon("fa-tachometer-alt")
                .main());

        mod.addPage(new Page("projects","Projects","classpath:/zk/pages/projects.zul")
                .featured()
                .icon("fa-tasks")
                .main());


        return mod;
    }
}
