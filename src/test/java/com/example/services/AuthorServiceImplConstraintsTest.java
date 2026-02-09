package com.example.services;

import com.example.AbstractIntegrationTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that validation constraints on {@link AuthorService} methods
 * trigger {@link ConstraintViolationException}.
 */
class AuthorServiceImplConstraintsTest extends AbstractIntegrationTest {

    @Inject
    AuthorService authorService;

    @ParameterizedTest(name = "saveAuthor(\"{0}\") triggers ConstraintViolationException")
    @MethodSource("saveAuthorInvalidArgs")
    void saveAuthorTriggersConstraintViolation(String name, String field, String errorMessage) {
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> authorService.saveAuthor(name));

        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getPropertyPath().toString().contains(field));
        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getMessage().contains(errorMessage));
    }

    static Stream<Arguments> saveAuthorInvalidArgs() {
        return Stream.of(
                Arguments.of("", "saveAuthor.name", "must not be blank"),
                Arguments.of(null, "saveAuthor.name", "must not be blank")
        );
    }

    @ParameterizedTest(name = "addBookToAuthor({0}) triggers ConstraintViolationException")
    @MethodSource("addBookToAuthorInvalidArgs")
    void addBookToAuthorTriggersConstraintViolation(SaveBook saveBook, String field, String errorMessage) {
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> authorService.addBookToAuthor(saveBook));

        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getPropertyPath().toString().contains(field));
        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getMessage().contains(errorMessage));
    }

    static Stream<Arguments> addBookToAuthorInvalidArgs() {
        return Stream.of(
                Arguments.of(null, "addBookToAuthor.saveBook", "must not be null")
        );
    }

    @ParameterizedTest(name = "findAuthorByName(\"{0}\") triggers ConstraintViolationException")
    @MethodSource("findAuthorByNameInvalidArgs")
    void findAuthorByNameTriggersConstraintViolation(String name, String field, String errorMessage) {
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> authorService.findAuthorByName(name));

        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getPropertyPath().toString().contains(field));
        assertThat(ex.getConstraintViolations())
                .anyMatch(v -> v.getMessage().contains(errorMessage));
    }

    static Stream<Arguments> findAuthorByNameInvalidArgs() {
        return Stream.of(
                Arguments.of("", "findAuthorByName.name", "must not be blank"),
                Arguments.of(null, "findAuthorByName.name", "must not be blank")
        );
    }
}
