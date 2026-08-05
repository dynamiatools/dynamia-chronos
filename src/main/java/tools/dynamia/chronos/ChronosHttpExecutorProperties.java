package tools.dynamia.chronos;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Sizing for the shared HTTP client used by every scheduled cron job / test
 * run (see {@code ChronosHttpClientConfig}). Externalized so an operator can
 * tune it per deployment via env vars, instead of each execution silently
 * spinning up its own client/thread pool.
 */
@ConfigurationProperties(prefix = "chronos.http")
public class ChronosHttpExecutorProperties {

    /**
     * Size of the bounded thread pool backing the shared {@link java.net.http.HttpClient}.
     * Caps how many HTTP calls Chronos can have in flight at once across all
     * projects, regardless of how many cron jobs happen to fire at the same
     * cron tick — protects the JVM from unbounded thread growth under a
     * "everything runs at :00" stampede.
     */
    private int threadPoolSize = 20;

    /**
     * TCP connect timeout for the shared client. Distinct from each job's
     * own {@code serverTimeout} (whole request/response window) — a job's
     * serverTimeout always takes priority for what it governs; this only
     * bounds how long establishing the connection itself can take.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
