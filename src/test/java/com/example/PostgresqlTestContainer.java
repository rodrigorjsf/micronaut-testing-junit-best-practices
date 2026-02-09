package com.example;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Singleton Testcontainer for PostgreSQL, shared across all tests.
 * The container is started once and reused between test classes to avoid
 * the overhead of starting a new container for each test.
 */
public final class PostgresqlTestContainer {

    private static PostgreSQLContainer container;

    private PostgresqlTestContainer() {
    }

    public static void init() {
        if (container == null) {
            container = new PostgreSQLContainer("postgres:16-alpine")
                    .withExposedPorts(5432)
                    .withReuse(true)
                    .withLabel("com.example.demo", "postgresqlTestcontainersReuse");
            container.start();
        }
    }

    public static String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    public static String getUsername() {
        return container.getUsername();
    }

    public static String getPassword() {
        return container.getPassword();
    }
}
