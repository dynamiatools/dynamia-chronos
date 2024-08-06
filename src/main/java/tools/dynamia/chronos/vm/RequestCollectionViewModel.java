package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import tools.dynamia.chronos.ChronosHttpMethod;
import tools.dynamia.chronos.ChronosHttpRequestExecutor;
import tools.dynamia.chronos.ChronosHttpResponse;
import tools.dynamia.chronos.domain.RequestCollection;
import tools.dynamia.chronos.domain.RequestItem;
import tools.dynamia.chronos.domain.UserRole;
import tools.dynamia.chronos.domain.Variable;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.domain.ValidationError;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.domain.util.LabelValue;
import tools.dynamia.zk.AbstractViewModel;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.util.ZKUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Init(superclass = true)
public class RequestCollectionViewModel extends AbstractViewModel<RequestCollection> {

    private LoggingService logger = new SLF4JLoggingService(RequestCollectionViewModel.class);
    private ProjectsViewModel projectsVM;
    private EntityTreeNode<SimpleEntity> node;
    private UserRole role;


    @Override
    protected void afterInitDefaults() {
        System.out.println(getModel());
        setModel(crudService().load(RequestCollection.class, getModel().getId()));
        this.projectsVM = (ProjectsViewModel) ZKUtil.getExecutionArg("viewModel");
        this.node = (EntityTreeNode<SimpleEntity>) ZKUtil.getExecutionArg("node");
        this.role = (UserRole) ZKUtil.getExecutionArg("role");

    }


    @Command
    public void save() {
        try {
            crudService().executeWithinTransaction(() -> {
                setModel(crudService().save(getModel()));
            });
            node.setEntity(getModel());
            node.setLabel(getModel().getTitle());
            notifyChanges();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Command
    public void addVariable() {
        var variable = new Variable("new var", "value");
        variable.setCollection(getModel());
        getModel().getVariables().add(variable);
        notifyChanges();
    }


    @Command
    public void removeVariable(@BindingParam Variable variable) {
        variable.setCollection(null);
        getModel().getVariables().remove(variable);
        save();
        notifyChanges();

    }

}
