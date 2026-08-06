package tools.dynamia.chronos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.Project;
import tools.dynamia.domain.services.CrudService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Full Spring context boot test, backed by a real MySQL running in a
 * Testcontainers container instead of whatever DATABASE_URL happens to be
 * configured. Makes this test runnable in CI/any environment with a Docker
 * daemon, without needing a pre-existing external database.
 */
@Testcontainers
@SpringBootTest
class DynamiaChronosApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

	@Autowired
	private CrudService crudService;

	@Autowired
	private CronJobScheduler cronJobScheduler;

	@Test
	void contextLoads() {
	}

	@Test
	void reschedulingActiveJobsWithAProjectDoesNotFailOnLazyLoading() {
		Project project = new Project("test-project-" + System.nanoTime());
		crudService.create(project);

		CronJob cronJob = new CronJob(project);
		cronJob.setName("test-job-" + System.nanoTime());
		cronJob.setServerHost("http://127.0.0.1:1");
		cronJob.setCronExpression("0 0 0 1 1 *"); // once a year, won't fire during the test
		crudService.create(cronJob);

		// Regression test: CronJobScheduler.configureTasks() (and the
		// next-execution gauge it refreshes) used to run outside any
		// Hibernate session when triggered at app startup, so touching
		// cronJob.getProject().getName() blew up with
		// LazyInitializationException the moment there was a real active
		// job with a project in the DB. reload() exercises the same path.
		assertDoesNotThrow(cronJobScheduler::reload);
	}

}
