package tools.dynamia.chronos.listeners;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.notificators.NotificationSender;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.integration.sterotypes.Listener;

import java.util.List;

@Listener
public class CronJobExecutionNotificationSenderListener implements CronJobExecutionListener {

    private LoggingService logger = new SLF4JLoggingService(CronJobExecutionNotificationSenderListener.class);
    private final ProjectService projectService;

    public CronJobExecutionNotificationSenderListener(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public void afterExecution(CronJob cronJob, CronJobLog log) {
        logger.info("Sending notification for: " + cronJob);
        List<Notificator> notificators = projectService.getNotificators(cronJob.getProject());
        notificators.stream().filter(Notificator::isActive)
                .forEach(notificator -> {
                    try {
                        NotificationSender sender = projectService.findNotificationSender(notificator.getSender());
                        if (sender != null) {
                            logger.info("Running " + sender);
                            sender.send(cronJob, log, notificator);
                        }
                    } catch (Exception e) {
                        logger.error("Error sending notification of cron job [" + cronJob.getName() + "] using: " + notificator.getSender(), e);
                    }
                });
    }
}
