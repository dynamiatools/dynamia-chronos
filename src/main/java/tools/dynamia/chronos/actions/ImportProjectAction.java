package tools.dynamia.chronos.actions;

import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Messagebox;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.domain.RequestCollection;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.commons.StringUtils;
import tools.dynamia.crud.AbstractCrudAction;
import tools.dynamia.crud.CrudActionEvent;
import tools.dynamia.domain.ValidationError;
import tools.dynamia.ui.MessageType;
import tools.dynamia.ui.UIMessages;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@InstallAction
public class ImportProjectAction extends AbstractCrudAction {

    public ImportProjectAction() {
        setName("Import");
        setApplicableClass(Project.class);
        setImage("upload");
    }

    @Override
    public void actionPerformed(CrudActionEvent evt) {
        Fileupload.get(event -> {
            var media = event.getMedia();
            if (media.getName().endsWith(".json")) {
                try {
                    Project project = StringPojoParser.createJsonMapper().readValue(media.getStreamData(), Project.class);
                    project.setName(project.getName() + " (imported)");
                    project.setId(null);
                    if (project.getCronjobs() != null) {
                        project.getCronjobs().forEach(c -> {
                            c.setProject(project);
                            c.setActive(false);
                            c.setExecutionsCount(0);
                            c.setStatus(null);
                            c.setCreatedAt(LocalDateTime.now());
                            c.setLastExecution(null);
                            c.setId(null);
                        });
                    }
                    if (project.getVariables() != null) {
                        project.getVariables().forEach(v -> {
                            v.setId(null);
                            v.setProject(project);
                        });
                    }
                    if (project.getNotificators() != null) {
                        project.getNotificators().forEach(n -> {
                            n.setId(null);
                            n.setProject(project);
                        });
                    }
                    if (project.getCollections() != null) {
                        project.getCollections().forEach(collection -> {
                            collection.setProject(project);
                            resetCollectionIds(collection);

                        });
                    }


                    crudService().executeWithinTransaction(project::save);
                    UIMessages.showMessage(project.getName());
                    evt.getController().doQuery();
                } catch (ValidationError e) {
                    Messagebox.show(e.getMessage());
                } catch (Exception e) {
                    Messagebox.show("Error importing: " + e.getMessage());
                }
            } else {
                UIMessages.showMessage("Invalid file", MessageType.WARNING);
            }
        });
    }

    private void resetCollectionIds(RequestCollection collection) {
        collection.setId(null);
        collection.checkHeaders();
        collection.getRequests().forEach(r -> {
            r.setId(null);
            r.setCollection(collection);
            r.checkHeaders();
            r.checkParams();
        });

        if (collection.getCollections() != null) {
            collection.getCollections().forEach(subc -> {
                subc.setParentCollection(collection);
                resetCollectionIds(subc);
            });
        }
        if (collection.getVariables() != null) {
            collection.getVariables().forEach(v -> {
                v.setId(null);
                v.setCollection(collection);
            });
        }
    }
}
