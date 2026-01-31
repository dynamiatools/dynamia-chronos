package tools.dynamia.chronos.actions;

import org.zkoss.zul.Filedownload;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.commons.StringUtils;
import tools.dynamia.crud.AbstractCrudAction;
import tools.dynamia.crud.CrudActionEvent;
import tools.dynamia.ui.MessageType;
import tools.dynamia.ui.UIMessages;

@InstallAction
public class ExportProjectAction extends AbstractCrudAction {

    public ExportProjectAction() {
        setName("Export");
        setApplicableClass(Project.class);
        setMenuSupported(true);
        setImage("download");
    }

    @Override
    public void actionPerformed(CrudActionEvent evt) {
        Project project = (Project) evt.getData();
        if (project != null) {

            crudService().executeWithinTransaction(() -> {
                Project target = crudService().load(Project.class, project.getId());
                String json = target.toJson();
                Filedownload.save(json, "application/json", StringUtils.simplifiedString(project.getName()) + "-chronos-project.json");
                UIMessages.showMessage("Exporting..");
            });

        } else {
            UIMessages.showMessage("Select project", MessageType.WARNING);
        }
    }
}
