package tools.dynamia.chronos.notificators;

import org.springframework.scheduling.annotation.Async;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.integration.sterotypes.Provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static tools.dynamia.commons.MapBuilder.put;

/**
 * Slack notification sender. Its use {@link Notificator} contact as webhook url
 */
@Provider
public class SlackNotificationSender implements NotificationSender {

    private LoggingService logger = new SLF4JLoggingService(SlackNotificationSender.class);

    @Override
    public String getId() {
        return "slack";
    }

    @Override
    public String getName() {
        return "Slack Webhook";
    }

    @Override
    @Async
    public void send(CronJob cronJob, CronJobLog log, Notificator notificator) {
        if (cronJob.isNotifyFails() && log.isFail()) {
            sendMessage(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] fails with status " + log.getStatus(), log.toHtml());
        }

        if (cronJob.isNotifyExecutions() && log.isExecuted()) {
            sendMessage(notificator.getContact(), "JOB [" + cronJob.getId() + " - " + cronJob.getName() + "] executed at " + log.getStartDate(), log.toHtml());
        }
    }

    private void sendMessage(String webhookURL, String subject, String content) {
        try {
            Map<String, Object> message = put(
                    "text", subject,
                    "blocks", List.of(
                            put("type", "section",
                                    "text", put("type", "html"
                                            , "text", content))
                    ));

            String json = StringPojoParser.convertMapToJson(message);

            logger.info("Sending notification to:  " + webhookURL + "/n" + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-type", "application/json")
                    .build();

            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Slack Status: " + response.statusCode());
        } catch (Exception e) {
            logger.error("Error sending slack notification", e);

        }

    }
}
