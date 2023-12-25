package tools.dynamia.chronos.notificators;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;

public interface NotificationSender {

    String getId();

    String getName();

    void send(CronJob cronJob, CronJobLog log, Notificator notificator);
}
