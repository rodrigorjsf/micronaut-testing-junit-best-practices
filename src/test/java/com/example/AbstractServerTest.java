package com.example;

import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;

/**
 * Base class for integration tests that need an HTTP client.
 * <p>
 * Equivalent to the original Groovy {@code EmbeddedServerSpecification}.
 * The embedded server is auto-started by {@code @MicronautTest}.
 * </p>
 */
public abstract class AbstractServerTest extends AbstractIntegrationTest {

    @Inject
    @Client("/")
    protected HttpClient httpClient;

    protected BlockingHttpClient getClient() {
        return httpClient.toBlocking();
    }
}
