package com.example.services;

import com.example.AbstractIntegrationTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that validation constraints on {@link SaveBook} fields work correctly.
 */
class SaveBookConstraintsTest extends AbstractIntegrationTest {

    @Inject
    Validator validator;

    static Stream<Arguments> invalidSaveBookArgs() {
        return Stream.of(
                Arguments.of("", 199, 1L, "title", "must not be blank"),
                Arguments.of(null, 199, 1L, "title", "must not be blank"),
                Arguments.of("title", 0, 1L, "pages", "must be greater than or equal to 1"),
                Arguments.of("title", 1, null, "authorId", "must not be null")
        );
    }

    @Test
    void validSaveBookHasNoViolations() {
        SaveBook saveBook = new SaveBook("Carrie", 199, 1L);

        Set<ConstraintViolation<SaveBook>> violations = validator.validate(saveBook);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "validation error for field \"{3}\": {4}")
    @MethodSource("invalidSaveBookArgs")
    void invalidSaveBookTriggersValidationError(String title, int pages, Long authorId,
                                                String field, String errorMessage) {
        SaveBook saveBook = new SaveBook(title, pages, authorId);

        Set<ConstraintViolation<SaveBook>> violations = validator.validate(saveBook);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().contains(field));
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains(errorMessage));
    }
}
