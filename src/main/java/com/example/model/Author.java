package com.example.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO for {@link com.example.entities.AuthorEntity}.
 */
@Serdeable
public class Author {

    @Schema(required = true, description = "The author id", example = "42")
    private final Long id;

    @Schema(required = true, description = "The author name", example = "Stephen King")
    private final String name;

    @JsonInclude
    private final List<Book> books;

    public Author(Long id, String name, List<Book> books) {
        this.id = id;
        this.name = name;
        this.books = books;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Book> getBooks() {
        return books;
    }

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", books=" + books +
                '}';
    }
}
