package tools.dynamia.chronos.notificators;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.modules.email.EmailMessage;
import tools.dynamia.modules.email.services.EmailService;

/**
 * Email notification sender. Its use {@link Notificator} contact as email address
 */
@Provider
public class EmailNotificationSender implements NotificationSender {

    private final EmailService emailService;

    public EmailNotificationSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String getId() {
        return "email";
    }

    @Override
    public String getName() {
        return "Email";
    }

    @Override
    public void send(CronJob cronJob, CronJobLog log, Notificator notificator) {
        if (cronJob.isNotifyFails() && log.isFail()) {
            sendEmail(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] fails with status " + log.getStatus(), log.toHtml());
        }

        if (cronJob.isNotifyExecutions() && log.isExecuted()) {
            sendEmail(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] executed at " + log.getStartDate(), log.toHtml());
        }
    }

    private void sendEmail(String emailAddress, String subject, String content) {
        EmailMessage message = new EmailMessage();
        message.setNotification(true);
        message.setSubject(subject);
        message.setTo(emailAddress);
        message.setContent(content);
        emailService.send(message);
    }
}
