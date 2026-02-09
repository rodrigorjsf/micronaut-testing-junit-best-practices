package com.example.controllers;

import com.example.AbstractServerTest;
import com.example.fixtures.AuthorFixture;
import com.example.model.Author;
import com.example.services.AuthorService;
import com.example.services.SaveBook;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * In this test we simulate that there is an error when saving the author:
 * <ul>
 *   <li>The {@code AuthorServiceMock} bean defined in this class is only used for this test
 *       because of the {@code @Requires} annotation and it throws an exception.</li>
 *   <li>The test checks that the server returns a 500 because the exception is thrown.</li>
 *   <li>We don't need to check that the author has not been created because if the mock is not
 *       working then the leakage detector will detect it and the test would fail.</li>
 * </ul>
 */
class AuthorControllerMockServiceTest extends AbstractServerTest implements AuthorFixture {

    @Override
    protected String getSpecName() {
        return "AuthorControllerMockServiceTest";
    }

    @Test
    void createAuthorFailsWhenServiceThrows() {
        CreateAuthorRequest createAuthorRequest = createAuthorRequest();
        HttpRequest<?> request = HttpRequest.POST("/authors", createAuthorRequest);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> getClient().exchange(request, Author.class));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Singleton
    @Primary
    @Requires(property = "spec.name", value = "AuthorControllerMockServiceTest")
    static class AuthorServiceMock implements AuthorService {

        @Override
        public Author saveAuthor(@NotBlank String name) {
            throw new RuntimeException("There was an error saving the author.");
        }

        @Override
        public void addBookToAuthor(@NotNull @Valid SaveBook saveBook) {
            // not used in this test
        }

        @Override
        public Optional<Author> findAuthorByName(@NotBlank String name) {
            // not used in this test
            return Optional.empty();
        }
    }
}
