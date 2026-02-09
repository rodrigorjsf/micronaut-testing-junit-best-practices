package com.example.controllers;

import com.example.AbstractServerTest;
import com.example.fixtures.AuthorFixture;
import com.example.model.Author;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorControllerTest extends AbstractServerTest implements AuthorFixture {

    @Test
    void createAuthorReturns201() {
        CreateAuthorRequest createAuthorRequest = createAuthorRequest();
        HttpRequest<?> request = HttpRequest.POST("/authors", createAuthorRequest);

        HttpResponse<Author> response = getClient().exchange(request, Author.class);
        assertEquals(HttpStatus.CREATED, response.status());

        Author author = response.body();
        assertThat(author).isNotNull();
        assertThat(author.getId()).isNotNull();
        assertThat(author.getName()).isEqualTo(createAuthorRequest.getName());
        assertThat(author.getBooks()).isEmpty();

        authorRepository.deleteAll();
    }

    @Test
    void createAuthorWithoutMandatoryParamsReturnsBadRequest() {
        CreateAuthorRequest createAuthorRequest = createAuthorRequest(null);
        HttpRequest<?> request = HttpRequest.POST("/authors", createAuthorRequest);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> getClient().exchange(request, Argument.of(Author.class), Argument.of(Map.class)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
}
