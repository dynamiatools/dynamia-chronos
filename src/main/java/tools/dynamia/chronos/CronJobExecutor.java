package tools.dynamia.chronos;

import org.springframework.http.HttpStatus;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Variable;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;


public class CronJobExecutor {

    private final CronJob cronJob;
    private final List<Variable> variables;
    private final Consumer<String> logger;


    public CronJobExecutor(CronJob cronJob, List<Variable> variables, Consumer<String> logger) {
        this.cronJob = cronJob;
        this.variables = variables;
        this.logger = logger;
    }

    public CronJobLog execute() {
        logger.accept("Executing cron job ");
        CronJobLog log = new CronJobLog(cronJob);

        var parsedServerPath = parse(cronJob.getServerHost());
        log.setServerHost(parsedServerPath);
        if (parsedServerPath.startsWith("http")) {
            executeHttp(log, parsedServerPath);
        } else {
            checkConnection(log, parsedServerPath);
        }


        log.setExecuted(true);
        log.setEndDate(LocalDateTime.now());
        log.setDuration(Duration.between(log.getStartDate(), log.getEndDate()));

        logger.accept("["+cronJob.getName()+"] job executed with status: "+log.getStatus());
        return log;
    }


    /**
     * Simple 'mustache like' template parser that replace all {{variable}} expression with variable value.
     *
     * @param template
     * @return
     */
    private String parse(final String template) {
        String result = template;
        for (var variable : variables) {
            result = result.replace("{{" + variable.getName() + "}}", variable.getValue());
        }
        return result;
    }

    /**
     * Execute a simple HTTP Request using cron job configuration
     *
     * @param log
     * @param url
     */
    private void executeHttp(CronJobLog log, String url) {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(cronJob.getServerTimeout());

        addHeader(request, "Authorization", cronJob.getServerAuthorization());
        addHeader(request, "Content-Type", cronJob.getContentType());

        if (cronJob.getRequestBody() != null && !cronJob.getRequestBody().isBlank()) {
            request.POST(HttpRequest.BodyPublishers.ofString(cronJob.getRequestBody(), StandardCharsets.UTF_8));
        }

        try {
            logger.accept("Executing http request " + url);
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            HttpStatus status = HttpStatus.resolve(response.statusCode());
            if (status == null) {
                status = HttpStatus.NO_CONTENT;
            }
            log.setStatus(status.value() + " - " + status.getReasonPhrase());
            log.setFail(status != HttpStatus.OK);
            log.setResponse(response.body());
            logger.accept("HTTP Response " + log.getResponse());
        } catch (Exception e) {
            log.setFail(true);
            log.setDetails(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Add and parse request header
     *
     * @param request
     * @param header
     * @param value
     */
    private void addHeader(HttpRequest.Builder request, String header, final String value) {
        if (value != null && !value.isBlank()) {
            var parsedValue = parse(value);
            request.header(header, parsedValue);
        }
    }

    /**
     * Start a socket connection with server host and post
     *
     * @param log
     * @param parserServerHost
     */
    private void checkConnection(CronJobLog log, String parserServerHost) {
        if (cronJob.getServerPort()!=null && cronJob.getServerPort() > 0) {
            logger.accept("Checking connection to " + parserServerHost + ":" + cronJob.getServerPort());
            try (Socket socket = new Socket(parserServerHost, cronJob.getServerPort())) {
                log.setStatus("Connected");
            } catch (IOException e) {
                log.setStatus("Cannot connect to server");
                log.setFail(true);
                log.setDetails(e.getMessage());
            }
            logger.accept(log.getStatus());
        } else {
            log.setFail(true);
            log.setStatus("Invalid Port");

        }
    }
}
