package tools.dynamia.chronos.notificators;

import org.springframework.scheduling.annotation.Async;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Notificator;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.chronos.services.ProjectService;
import tools.dynamia.commons.logger.LoggingService;
import tools.dynamia.commons.logger.SLF4JLoggingService;
import tools.dynamia.integration.sterotypes.Provider;
import tools.dynamia.ui.MessageType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Slack notification sender. Its use {@link Notificator} contact as webhook url
 */
@Provider
public class SlackNotificationSender implements NotificationSender {

    private LoggingService logger = new SLF4JLoggingService(SlackNotificationSender.class);
    private final ProjectService projectService;

    public SlackNotificationSender(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public String getId() {
        return "slack";
    }

    @Override
    public String getName() {
        return "Slack Webhook";
    }

    @Override
    public String getIcon() {
        return "fab fa-slack";
    }

    @Override
    @Async
    public void send(CronJob cronJob, CronJobLog log, Notificator notificator) {
        Project project = projectService.getById(cronJob.getProject().getId());
        String title = project.getName() + " - Cron Job Failed: " + cronJob.getName();
        StringBuilder content = new StringBuilder();

        content.append("*Duration:* ").append(log.getDuration().toMillis()).append("ms\n");
        content.append("*Status:* ").append(log.getStatus()).append("\n");
        if (log.getDetails() != null) {
            content.append("*Details:* ").append(log.getDetails()).append("\n");
        }
        content.append("*URL:* ").append(log.getServerHost()).append("\n");


        if (cronJob.isNotifyExecutions() && log.isExecuted()) {
            title = project.getName() + " - Cron Job Executed: " + cronJob.getName();
            sendMessage(notificator.getContact(), title, content.toString(), MessageType.NORMAL);
        }

        if (cronJob.isNotifyFails() && log.isFail()) {
            sendMessage(notificator.getContact(), title, content.toString(), MessageType.ERROR);
        }


    }

    private void sendMessage(String webhookURL, String subject, String content, MessageType messageType) {
        try {
            String icon = "red_circle";
            String unicode = "1f534";

            switch (messageType) {
                case ERROR -> {
                    icon = "red_circle";
                    unicode = "1f534";
                }
                case NORMAL, INFO -> {
                    icon = "white_check_mark";
                    unicode = "2705";
                }
                case WARNING, CRITICAL -> {
                    icon = "warning";
                    unicode = "26a0-fe0f";
                }
                case SPECIAL -> {
                    icon = "smile_cat";
                    unicode = "1f638";
                }
            }


            String message = """
                    {
                    	"blocks": [
                    		{
                    			"type": "rich_text",
                    			"elements": [
                    				{
                    					"type": "rich_text_section",
                    					"elements": [
                    						{
                    							"type": "emoji",
                    							"name": "%s",
                    							"unicode": "%s"
                    						},
                    						{
                    							"type": "text",
                    							"text": " %s"
                    						}
                    					]
                    				}
                    			]
                    		},
                    		{
                    			"type": "section",
                    			"text": {
                    				"type": "mrkdwn",
                    				"text": "%s"
                    			}
                    		}
                    	]
                    }
                    """.formatted(icon, unicode, subject, content);


            logger.info("Sending notification to:  " + webhookURL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookURL))
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .header("Content-type", "application/json")
                    .build();

            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Slack Status: " + response.statusCode());
        } catch (Exception e) {
            logger.error("Error sending slack notification", e);

        }

    }
}
