package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.domain.RequestCollection;
import tools.dynamia.chronos.domain.UserRole;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.zk.AbstractViewModel;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.util.ZKUtil;

@Init(superclass = true)
public class ProjectViewModel extends AbstractViewModel<Project> {

    private LoggingService logger = new SLF4JLoggingService(ProjectViewModel.class);
    private ProjectsViewModel projectsVM;
    private EntityTreeNode<SimpleEntityUuid> node;
    private UserRole role;


    @Override
    protected void afterInitDefaults() {
        System.out.println(getModel());
        setModel(crudService().load(Project.class, getModel().getId()));
        this.projectsVM = (ProjectsViewModel) ZKUtil.getExecutionArg("viewModel");
        this.node = (EntityTreeNode<SimpleEntityUuid>) ZKUtil.getExecutionArg("node");
        this.role = (UserRole) ZKUtil.getExecutionArg("role");

    }


    @Command
    public void save() {
        try {
            crudService().executeWithinTransaction(() -> {
                setModel(crudService().save(getModel()));
            });
            node.setEntity(getModel());
            node.setLabel(getModel().getName());
            notifyChanges();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Command
    public void addVariable() {
        var variable = new Variable("new var", "value");
        variable.setProject(getModel());
        getModel().getVariables().add(variable);
        notifyChanges();
    }

    @Command
    public void removeVariable(@BindingParam Variable variable) {
        variable.setProject(null);
        getModel().getVariables().remove(variable);
        save();
        notifyChanges();

    }

}
