package tools.dynamia.chronos.vm;

import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.zk.crud.ui.EntityTreeNode;

public class ProjectsViewModel extends AbstractProjectsViewModel {

    @Init
    public void init() {
        super.init();
    }

    @Override
    protected void loadMoreNodes(Project project, EntityTreeNode<SimpleEntityUuid> projectNode) {

    }

    @Override
    @Command
    public void nodeSelected() {

    }
}
