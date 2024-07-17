package tools.dynamia.chronos.vm;

import org.apache.xmlbeans.impl.common.IOUtil;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import tools.dynamia.chronos.domain.*;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.domain.AbstractEntity;
import tools.dynamia.domain.CrudServiceAware;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.io.IOUtils;
import tools.dynamia.ui.UIMessages;
import tools.dynamia.zk.crud.ui.EntityTreeNode;
import tools.dynamia.zk.ui.Import;
import tools.dynamia.zk.viewers.ui.Viewer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class ProjectsViewModel extends AbstractProjectsViewModel implements CrudServiceAware {

    private Component view;
    private Tabbox tabbox;


    @Init
    public void init() {
        super.init();
    }

    @Override
    protected void loadMoreNodes(Project project, EntityTreeNode<SimpleEntityUuid> projectNode) {

    }

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {
        this.view = view;
        this.tabbox = (Tabbox) view.query("tabbox");
    }

    @Override
    @Command
    public void nodeSelected() {
        if (getSelectedNode() != null) {
            UserRole userRole = UserRole.Reader;
            if (getSelectedNode().getRole() instanceof UserRole role) {
                userRole = role;
            }

            if (getSelectedNode().getEntity() instanceof AbstractEntity<?> entity && entity.getId() != null) {
                switch (entity) {
                    case Project project -> showProject(project, userRole);
                    case CronJob cronJob -> showCronJob(cronJob, userRole);
                    case RequestCollection collection -> showRequestCollection(collection, userRole);
                    case RequestItem requestItem -> showRequestItem(requestItem, userRole);
                    default -> System.out.println("Something else selected");
                }
            }

            notifyChanges();

        }

    }

    private Tabpanel createOrSelectPanel(SimpleEntityUuid entity, String label, Component content) {

        var tabpanel = (Tabpanel) tabbox.getTabpanels()
                .getChildren()
                .stream()
                .filter(tp -> entity.equals(tp.getAttribute("entity")))
                .findFirst().orElseGet(() -> {

                    Tab tab = new Tab(label);
                    tab.setClosable(true);
                    tabbox.getTabs().appendChild(tab);
                    Tabpanel tabPanel = new Tabpanel();
                    tabPanel.setAttribute("entity", entity);
                    tabPanel.appendChild(content);
                    tabbox.getTabpanels().appendChild(tabPanel);
                    return tabPanel;
                });

        tabpanel.getLinkedTab().setSelected(true);
        return tabpanel;
    }


    private void showProject(Project project, UserRole userRole) {
        Viewer viewer = new Viewer("form", Project.class, project);
        viewer.setReadonly(userRole == UserRole.Reader);
        createOrSelectPanel(project, project.getName(), viewer);
    }

    private void showCronJob(CronJob cronJob, UserRole userRole) {
        Viewer viewer = new Viewer("form", CronJob.class, cronJob);
        viewer.setReadonly(userRole == UserRole.Reader);
        createOrSelectPanel(cronJob, cronJob.getName(), viewer);
    }

    private void showRequestCollection(RequestCollection collection, UserRole userRole) {
        Viewer viewer = new Viewer("form", RequestCollection.class, collection);
        viewer.setReadonly(userRole == UserRole.Reader);
        createOrSelectPanel(collection, collection.getTitle(), viewer);
    }

    private void showRequestItem(RequestItem requestItem, UserRole userRole) {
        Import content = new Import();
        content.setSrc("classpath:/zk/pages/request.zul");
        content.addArg("entity", requestItem);
        content.addArg("userRole", userRole);
        content.addArg("node", getSelectedNode());
        content.addArg("viewModel", this);
        content.setVflex("1");
        var panel = createOrSelectPanel(requestItem, requestItem.getHttpMethod() + " " + requestItem.getName(), content);
        panel.setHflex("1");

        content.addArg("panel", panel);
    }

    @Command
    public void addCronJob() {
        CronJob cronJob = new CronJob();
        var node = getSelectedNode();
        System.out.println(node);
    }

    @Command
    public void addCollection() {
        if (getSelectedNode() != null) {
            var entity = getSelectedNode().getEntity();
            RequestCollection collection = new RequestCollection();
            collection.setTitle("New Collection");

            Project project = (Project) getSelectedNode().getSource();
            if (entity instanceof RequestCollection parent) {
                if (parent.getId() == null && parent.getProject() != null) {
                    collection.setProject(parent.getProject());
                    project = parent.getProject();
                } else {
                    collection.setParentCollection(parent);
                }
            }

            Project finalProject = project;
            UIMessages.showInput("Name", String.class, s -> {
                collection.setTitle(s);
                crudService().executeWithinTransaction(() -> crudService().save(collection));
                var node = buildNode(collection, finalProject);
                getSelectedNode().addChild(node);
                getSelectedNode().open();
                notifyChanges();
            });

        }
    }

    @Command
    public void deleteCollection() {
        if (getSelectedNode().getEntity() instanceof RequestCollection collection && collection.getId() != null) {
            UIMessages.showQuestion("Are you sure to delete " + collection + "? All request and subfolders will be deleted", () -> {
                crudService().executeWithinTransaction(() -> crudService().delete(RequestCollection.class, collection.getId()));
                getSelectedNode().getParent().getChildren().remove(getSelectedNode());
                notifyChanges();
            });
        }
    }

    @Command
    public void importCollection() {
        Fileupload.get(event -> {
            var media = event.getMedia();
            if (media.getName().endsWith(".json")) {
                InputStream stream = media.getStreamData();

                String content = IOUtils.readContent(stream, StandardCharsets.UTF_8);

                RequestCollection collection = projectService.importCollectionFromPostman(content);
                Project project = (Project) getSelectedNode().getSource();
                collection.setProject(project);
                crudService().executeWithinTransaction(() -> crudService().save(collection));
                var node = buildNode(collection, project);
                getSelectedNode().addChild(node);
                getSelectedNode().open();
                notifyChanges();
            }
        });
    }


    @Command
    public void addRequest() {
        var node = getSelectedNode();
        System.out.println(node);
    }
}
