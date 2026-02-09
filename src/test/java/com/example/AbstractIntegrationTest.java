package com.example;

import com.example.repositories.AuthorRepository;
import com.example.repositories.BookRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for all integration tests.
 * <p>
 * Provides Testcontainers-backed PostgreSQL configuration via {@link TestPropertyProvider},
 * injects common repositories, and checks for data leakage after each test.
 * </p>
 * <p>
 * Equivalent to the original Groovy {@code ApplicationContextSpecification} which combined
 * {@code ConfigurationFixture}, {@code PostgresqlFixture}, {@code RepositoriesFixture},
 * and {@code LeakageDetector}.
 * </p>
 */
@Testcontainers(disabledWithoutDocker = true)
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest implements TestPropertyProvider {

    @Inject
    protected AuthorRepository authorRepository;

    @Inject
    protected BookRepository bookRepository;

    public AuthorRepository getAuthorRepository() {
        return authorRepository;
    }

    public BookRepository getBookRepository() {
        return bookRepository;
    }

    @Override
    @NonNull
    public Map<String, String> getProperties() {
        PostgresqlTestContainer.init();
        Map<String, String> props = new HashMap<>();
        props.put("datasources.default.url", PostgresqlTestContainer.getJdbcUrl());
        props.put("datasources.default.username", PostgresqlTestContainer.getUsername());
        props.put("datasources.default.password", PostgresqlTestContainer.getPassword());
        props.put("datasources.default.schema-generate", "CREATE_DROP");
        props.put("datasources.default.dialect", "POSTGRES");
        props.put("mockSecurityService", String.valueOf(mockSecurityServiceEnabled()));
        String specName = getSpecName();
        if (specName != null) {
            props.put("spec.name", specName);
        }
        return props;
    }

    /**
     * Whether the {@link com.example.security.MockSecurityService} should be activated.
     * Defaults to {@code true}. Override to {@code false} in tests that need the real security service.
     */
    protected boolean mockSecurityServiceEnabled() {
        return true;
    }

    /**
     * The spec name used for conditional bean activation via {@code @Requires(property = "spec.name", value = "...")}.
     * Defaults to {@code null} (no spec name set).
     */
    protected String getSpecName() {
        return null;
    }

    /**
     * Leakage detector: asserts the database is clean after each test.
     * Each test is responsible for cleaning up its own data.
     */
    @AfterEach
    void checkLeakage() {
        assertThat(bookRepository.count())
                .describedAs("Book leakage detected - ensure test cleans up its data")
                .isZero();
        assertThat(authorRepository.count())
                .describedAs("Author leakage detected - ensure test cleans up its data")
                .isZero();
    }
}
