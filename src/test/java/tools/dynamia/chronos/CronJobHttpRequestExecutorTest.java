package tools.dynamia.chronos;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronJobHttpRequestExecutorTest {

    private HttpServer httpServer;
    private ServerSocket rawSocket;

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (rawSocket != null) {
            try {
                rawSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void executesHttpRequestWhenHostIsAUrl() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();

        CronJob cronJob = new CronJob();
        cronJob.setName("http-job");
        cronJob.setServerHost("http://127.0.0.1:" + httpServer.getAddress().getPort());

        var log = new CronJobHttpRequestExecutor(cronJob, List.of(), msg -> {}).execute();

        assertInstanceOf(CronJobLog.class, log);
        assertTrue(log.isExecuted());
        assertFalse(log.isFail());
    }

    @Test
    void sendsCronJobCustomHeadersOnExecution() throws IOException {
        var lastRequestHeaders = new java.util.concurrent.atomic.AtomicReference<Headers>();

        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            lastRequestHeaders.set(exchange.getRequestHeaders());
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();

        CronJob cronJob = new CronJob();
        cronJob.setName("http-job");
        cronJob.setServerHost("http://127.0.0.1:" + httpServer.getAddress().getPort());
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", "abc123");
        headers.put("X-Custom", "chronos");
        cronJob.setHeaders(headers);

        var log = new CronJobHttpRequestExecutor(cronJob, List.of(), msg -> {}).execute();

        assertFalse(log.isFail());
        assertEquals("abc123", lastRequestHeaders.get().getFirst("X-Api-Key"));
        assertEquals("chronos", lastRequestHeaders.get().getFirst("X-Custom"));
    }

    @Test
    void connectsToOpenTcpPortWhenHostIsNotAUrl() throws IOException {
        rawSocket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));

        CronJob cronJob = new CronJob();
        cronJob.setName("tcp-job");
        cronJob.setServerHost("127.0.0.1");
        cronJob.setServerPort(rawSocket.getLocalPort());

        var log = new CronJobHttpRequestExecutor(cronJob, List.of(), msg -> {}).execute();

        assertEquals("Connected", log.getStatus());
        assertFalse(log.isFail());
    }

    @Test
    void failsWhenTcpPortIsClosed() {
        CronJob cronJob = new CronJob();
        cronJob.setName("tcp-job");
        cronJob.setServerHost("127.0.0.1");
        cronJob.setServerPort(1);
        cronJob.setServerTimeout(Duration.ofSeconds(1));

        var log = new CronJobHttpRequestExecutor(cronJob, List.of(), msg -> {}).execute();

        assertTrue(log.isFail());
        assertEquals("Cannot connect to server", log.getStatus());
    }

    @Test
    void failsWhenPortIsInvalid() {
        CronJob cronJob = new CronJob();
        cronJob.setName("tcp-job");
        cronJob.setServerHost("127.0.0.1");
        cronJob.setServerPort(null);

        var log = new CronJobHttpRequestExecutor(cronJob, List.of(), msg -> {}).execute();

        assertTrue(log.isFail());
        assertEquals("Invalid Port", log.getStatus());
    }
}
