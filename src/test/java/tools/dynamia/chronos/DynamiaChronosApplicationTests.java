package tools.dynamia.chronos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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

	@Test
	void contextLoads() {
	}

}
