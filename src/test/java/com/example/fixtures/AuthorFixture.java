package com.example.fixtures;

import com.example.controllers.CreateAuthorRequest;
import com.example.entities.AuthorEntity;
import com.example.repositories.AuthorRepository;

/**
 * Test fixture for creating {@link AuthorEntity} and {@link CreateAuthorRequest} instances.
 * <p>
 * Implement this interface in test classes that extend {@code AbstractIntegrationTest}
 * (which provides {@link #getAuthorRepository()}).
 * </p>
 */
public interface AuthorFixture {

    AuthorRepository getAuthorRepository();

    default AuthorEntity saveAuthor(String name) {
        AuthorEntity author = new AuthorEntity(name);
        return getAuthorRepository().save(author);
    }

    default AuthorEntity saveAuthor() {
        return saveAuthor("author name");
    }

    default CreateAuthorRequest createAuthorRequest(String name) {
        return new CreateAuthorRequest(name);
    }

    default CreateAuthorRequest createAuthorRequest() {
        return createAuthorRequest("Stephen King");
    }
}
