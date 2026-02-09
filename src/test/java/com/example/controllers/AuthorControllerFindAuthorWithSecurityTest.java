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
 * Same test as {@link AuthorControllerFindAuthorTest} but in this case we disable the mock
 * so this test uses the real security service implementation.
 * Search the log for "Using REAL security service".
 */
class AuthorControllerFindAuthorWithSecurityTest extends AbstractServerTest implements AuthorFixture {

    @Override
    protected boolean mockSecurityServiceEnabled() {
        return false;
    }

    @Test
    void findAuthorFailsBecauseNoAdminUser() {
        String authorName = "My favourite author";
        saveAuthor(authorName);

        URI uri = UriBuilder.of("/authors/by-name")
                .queryParam("author", authorName)
                .build();

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> getClient().exchange(HttpRequest.GET(uri), Author.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());

        authorRepository.deleteAll();
    }

    @Test
    void findAuthorByNameWithAdminUser() {
        String authorName = "My favourite author";
        saveAuthor(authorName);

        URI uri = UriBuilder.of("/authors/by-name")
                .queryParam("author", authorName)
                // Pass the admin param so the "SecurityServiceImpl" returns true
                .queryParam("username", "admin")
                .build();

        HttpResponse<Author> response = getClient().exchange(HttpRequest.GET(uri), Author.class);
        assertEquals(HttpStatus.OK, response.status());

        Author author = response.body();
        assertThat(author).isNotNull();
        assertThat(author.getName()).isEqualTo(authorName);

        authorRepository.deleteAll();
    }
}
