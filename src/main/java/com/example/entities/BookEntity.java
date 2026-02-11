package com.example.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.GeneratedValue.Type;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Book entity. */
@MappedEntity(value = "book", schema = "public")
public record BookEntity(
    @Id @GeneratedValue(value = Type.IDENTITY) @Nullable Long id,
    @NotBlank String title,
    @Min(1) int pages,
    @DateCreated @Nullable LocalDateTime dateCreated,
    @NotNull @JoinColumn @Relation(value = Relation.Kind.MANY_TO_ONE) @Nullable AuthorEntity author) {

  /**
   * Compact constructor for creating a BookEntity with title, pages, and author.
   * The id and dateCreated are managed by the database.
   */
  public BookEntity(@NotBlank String title, @Min(1) int pages, @NotNull AuthorEntity author) {
    this(null, title, pages, null, author);
  }
}
