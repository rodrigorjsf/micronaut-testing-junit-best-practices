package com.example.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.GeneratedValue.Type;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Set;

/** Author entity. */
@MappedEntity(value = "author", schema = "public")
public record AuthorEntity(
    @Id @GeneratedValue(value = Type.IDENTITY) @Nullable Long id,
    @NotBlank String name,
    @DateCreated @Nullable LocalDateTime dateCreated,
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "author") @Nullable Set<BookEntity> books) {

    /**
     * Compact constructor for creating an AuthorEntity with only a name.
     * The id and dateCreated are managed by the database.
     * The books set is initialized as an empty set.
     */
    public AuthorEntity(@NotBlank String name) {
        this(null, name, null, Set.of());
    }
}
