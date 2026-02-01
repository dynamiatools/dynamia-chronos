package tools.dynamia.chronos.notificators;

import org.springframework.scheduling.annotation.Async;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.integration.sterotypes.Provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Slack notification sender. Its use {@link Notificator} contact as webhook url
 */
@Provider
public class WebhookNotificationSender implements NotificationSender {

    private LoggingService logger = new SLF4JLoggingService(WebhookNotificationSender.class);
    private final ProjectService projectService;

    public WebhookNotificationSender(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public String getId() {
        return "webhook";
    }

    @Override
    public String getName() {
        return "HTTP Webhook";
    }

    @Override
    public String getIcon() {
        return "fa fa-globe";
    }

    @Override
    @Async
    public void send(CronJob cronJob, CronJobLog log, Notificator notificator) {
        sendMessage(notificator.getContact(), log);
    }

    private void sendMessage(String webhookURL, CronJobLog content) {
        try {


            if(content.getResponse()==null || content.getResponse().isBlank()){
                content.setResponse("\"empty\"");
            }
            String message = StringPojoParser.convertPojoToJson(content);


            logger.info("Sending webhook notification to:  " + webhookURL);

            logger.info("CONTENT: \n\n"+message);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .header("Content-type", "application/json")
                    .build();

            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Webhook Status: " + response.statusCode());
        } catch (Exception e) {
            logger.error("Error sending webhook notification", e);

        }

    }
}
