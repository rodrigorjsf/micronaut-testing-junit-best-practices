package com.example.controllers;

import com.example.AbstractServerTest;
import com.example.fixtures.AuthorFixture;
import com.example.model.Author;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for finding an author by name.
 * <p>
 * This test passes because by default we use the {@code MockSecurityService} that always returns true.
 * Run the test and search in the log for "Using mock security service".
 * </p>
 */
class AuthorControllerFindAuthorTest extends AbstractServerTest implements AuthorFixture {

    @Test
    void findAuthorByName() {
        String authorName = "My favourite author";
        saveAuthor(authorName);

        URI uri = UriBuilder.of("/authors/by-name")
                .queryParam("author", authorName)
                .build();

        HttpResponse<Author> response = getClient().exchange(HttpRequest.GET(uri), Author.class);
        assertEquals(HttpStatus.OK, response.status());

        Author author = response.body();
        assertThat(author).isNotNull();
        assertThat(author.getName()).isEqualTo(authorName);

        authorRepository.deleteAll();
    }

    @Test
    void findNonExistentAuthorReturns404() {
        URI uri = UriBuilder.of("/authors/by-name")
                .queryParam("author", "Iván")
                .build();

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> getClient().exchange(HttpRequest.GET(uri), Author.class));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
