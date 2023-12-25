package tools.dynamia.chronos.notificators;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.domain.ValidationError;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.modules.email.SMSMessage;
import tools.dynamia.modules.email.domain.EmailAccount;
import tools.dynamia.modules.email.services.EmailService;
import tools.dynamia.modules.email.services.SMSService;

/**
 * SMS notification sender. Its use {@link Notificator} contact as mobile phone number
 */
@Provider
public class SMSNotificationSender implements NotificationSender {

    private final EmailService emailService;
    private final SMSService smsService;

    public SMSNotificationSender(EmailService emailService, SMSService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Override
    public String getId() {
        return "sms";
    }

    @Override
    public String getName() {
        return "SMS";
    }

    @Override
    public void send(CronJob cronJob, CronJobLog log, Notificator notificator) {
        if (cronJob.isNotifyFails() && log.isFail()) {
            sendSMS(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] fails with status " + log.getStatus());
        }

        if (cronJob.isNotifyExecutions() && log.isExecuted()) {
            sendSMS(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] executed at " + log.getStartDate());
        }
    }

    private void sendSMS(String phoneNumber, String text) {
        EmailAccount smsAccount = emailService.getNotificationEmailAccount();
        if (smsAccount == null) {
            smsAccount = emailService.getPreferredEmailAccount();
        }

        if (smsAccount.getSmsUsername() == null) {
            throw new ValidationError("SMS account is not setup");
        }

        SMSMessage message = new SMSMessage();
        message.setPhoneNumber(phoneNumber);
        message.setText(text);
        message.setCredentials(smsAccount.getSmsUsername(), smsAccount.getSmsPassword(), smsAccount.getSmsRegion());
        smsService.send(message);
    }
}
