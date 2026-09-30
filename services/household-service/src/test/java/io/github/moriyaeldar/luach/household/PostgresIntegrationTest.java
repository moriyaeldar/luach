package io.github.moriyaeldar.luach.household;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL.
 *
 * <p>By default a disposable container is started with Testcontainers (this is what CI does). Set
 * {@code TEST_DATABASE_URL}, {@code TEST_DATABASE_USER} and {@code TEST_DATABASE_PASSWORD} to run against
 * an existing database instead, e.g. where Docker isn't available.
 */
public abstract class PostgresIntegrationTest {

    private static PostgreSQLContainer postgres;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USER"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
            return;
        }
        synchronized (PostgresIntegrationTest.class) {
            if (postgres == null) {
                postgres = new PostgreSQLContainer("postgres:16-alpine");
                postgres.start();
            }
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
