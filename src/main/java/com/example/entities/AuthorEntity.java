package com.example.entities;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.DateCreated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Author entity.
 */
@Introspected
@Entity
@Table(name = "author", schema = "public")
public class AuthorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column
    private String name;

    @Column
    @DateCreated
    private LocalDateTime dateCreated;

    @OneToMany(mappedBy = "author", targetEntity = BookEntity.class)
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity(@NotBlank String name) {
        this.name = name;
    }

    public AuthorEntity() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Set<BookEntity> getBooks() {
        return books;
    }

    public void setBooks(Set<BookEntity> books) {
        this.books = books;
    }

    @Override
    public String toString() {
        return "AuthorEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dateCreated=" + dateCreated +
                ", books=" + books +
                '}';
    }
}
