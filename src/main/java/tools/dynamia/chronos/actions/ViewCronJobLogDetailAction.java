package tools.dynamia.chronos.actions;

import tools.dynamia.actions.ActionEvent;
import tools.dynamia.actions.InstallAction;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.zk.viewers.table.TableViewRowAction;
import tools.dynamia.zk.viewers.ui.Viewer;

@InstallAction
public class ViewCronJobLogDetailAction extends TableViewRowAction {

    public ViewCronJobLogDetailAction() {
        setId("ViewCronJobLogDetail");
        setImage("add");
        setColor("white");
        setBackground(".bg-red");
        setSclass("btn-sm");
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getData() instanceof CronJobLog log) {
            Viewer viewer = new Viewer("form", CronJobLog.class, log);
            viewer.setReadonly(true);
            viewer.showModal("Log # " + log.getId());
        }
    }
}
