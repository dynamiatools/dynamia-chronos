package tools.dynamia.chronos;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;
import tools.dynamia.chronos.services.CronJobsService;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that {@link CronJobScheduler#executeWithLock(CronJob)} behaves as
 * it would across two app instances sharing a database: only one of them
 * actually runs a given job trigger, the other is skipped instead of running
 * it a second time. Uses an in-memory H2 DB standing in for the shared MySQL
 * so the test stays a pure unit test.
 */
class CronJobSchedulerLockTest {

    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cronjob-scheduler-lock-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        this.dataSource = ds;

        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE shedlock (
                        name VARCHAR(64) NOT NULL,
                        lock_until TIMESTAMP(3) NOT NULL,
                        locked_at TIMESTAMP(3) NOT NULL,
                        locked_by VARCHAR(255) NOT NULL,
                        PRIMARY KEY (name)
                    )
                    """);
        }
    }

    @Test
    void onlyOneInstanceExecutesTheSameJobConcurrently() throws InterruptedException {
        CronJob cronJob = new CronJob();
        cronJob.setId(1L);
        cronJob.setName("shared-job");
        cronJob.setServerTimeout(Duration.ofSeconds(5));

        AtomicInteger executions = new AtomicInteger();
        CronJobsService cronJobsService = slowCountingCronJobsService(executions);

        // Two schedulers standing in for two app instances: independent
        // LockProvider instances, same underlying shedlock table. The meter
        // registry is irrelevant to what this test verifies (locking), a
        // plain in-memory one is enough to satisfy the constructor.
        CronJobScheduler scheduler1 = new CronJobScheduler(cronJobsService, newLockProvider(), new SimpleMeterRegistry());
        CronJobScheduler scheduler2 = new CronJobScheduler(cronJobsService, newLockProvider(), new SimpleMeterRegistry());

        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Thread t1 = raceThread(scheduler1, cronJob, bothReady, go);
        Thread t2 = raceThread(scheduler2, cronJob, bothReady, go);

        t1.start();
        t2.start();
        bothReady.await(5, TimeUnit.SECONDS);
        go.countDown();
        t1.join(5000);
        t2.join(5000);

        assertEquals(1, executions.get(), "only one instance should have executed the job");
    }

    private LockProvider newLockProvider() {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }

    private Thread raceThread(CronJobScheduler scheduler, CronJob cronJob, CountDownLatch bothReady, CountDownLatch go) {
        return new Thread(() -> {
            bothReady.countDown();
            try {
                go.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler.executeWithLock(cronJob);
        });
    }

    private CronJobsService slowCountingCronJobsService(AtomicInteger executions) {
        return new CronJobsService() {
            @Override
            public List<CronJob> getActiveCronJobs() {
                return List.of();
            }

            @Override
            public List<CronJob> getFailingCronJobs() {
                return List.of();
            }

            @Override
            public void execute(CronJob cronJob) {
                executions.incrementAndGet();
                try {
                    // Hold the lock long enough that the losing thread's
                    // acquire attempt overlaps with this one still running.
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public CronJobLog test(CronJob cronJob) {
                throw new UnsupportedOperationException("not used in this test");
            }

            @Override
            public List<CronJobLog> getLatestLogs(CronJob cronJob) {
                return List.of();
            }

            @Override
            public long getActiveCronJobsCount() {
                return 0;
            }

            @Override
            public long countCronJobsByStatus(String status) {
                return 0;
            }
        };
    }
}
