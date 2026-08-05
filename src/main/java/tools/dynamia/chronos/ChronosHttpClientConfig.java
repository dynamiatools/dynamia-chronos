package tools.dynamia.chronos;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wires the shared, bounded HTTP client used by every scheduled cron job
 * execution (see {@link ChronosHttpRequestExecutor}). Replaces the previous
 * "new HttpClient per execution" behaviour, which left each run's default
 * cached thread pool to grow unbounded under concurrent job triggers.
 */
@Configuration
@EnableConfigurationProperties(ChronosHttpExecutorProperties.class)
public class ChronosHttpClientConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService chronosHttpExecutorService(ChronosHttpExecutorProperties properties) {
        return Executors.newFixedThreadPool(properties.getThreadPoolSize(), chronosThreadFactory());
    }

    @Bean
    public HttpClient chronosHttpClient(ChronosHttpExecutorProperties properties, ExecutorService chronosHttpExecutorService) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .executor(chronosHttpExecutorService)
                .build();
    }

    private static ThreadFactory chronosThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "chronos-http-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
