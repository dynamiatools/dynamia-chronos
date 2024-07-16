package tools.dynamia.chronos;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.domain.Variable;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;


public class CronJobHttpRequestExecutor extends ChronosHttpRequestExecutor {


    private final CronJob cronJob;

    public CronJobHttpRequestExecutor(CronJob cronJob, List<Variable> variables, Consumer<String> logger) {
        super(cronJob, variables, logger);
        this.cronJob = cronJob;
    }

    @Override
    protected ChronosHttpResponse newResponse() {
        return new CronJobLog(cronJob);
    }

    public ChronosHttpResponse execute() {
        logger.accept("Executing cron job ");


        var parsedServerPath = parse(request.getServerHost());

        ChronosHttpResponse log = null;
        if (parsedServerPath.startsWith("http")) {
            log = super.execute();
        } else {
            log = newResponse();
            checkConnection(log, parsedServerPath);
            return log;
        }


        logger.accept("[" + request.getName() + "] job executed with status: " + log.getStatus());
        return log;
    }


    /**
     * Start a socket connection with server host and post
     *
     * @param log
     * @param parserServerHost
     */
    private void checkConnection(ChronosHttpResponse log, String parserServerHost) {
        if (cronJob.getServerPort() != null && cronJob.getServerPort() > 0) {
            logger.accept("Checking connection to " + parserServerHost + ":" + cronJob.getServerPort());
            try (Socket socket = new Socket(parserServerHost, cronJob.getServerPort())) {
                log.setStatus("Connected");
            } catch (IOException e) {
                log.setStatus("Cannot connect to server");
                log.setFail(true);
                log.setResponse(e.getMessage());
            }
            logger.accept(log.getStatus());
        } else {
            log.setFail(true);
            log.setStatus("Invalid Port");

        }
    }
}
