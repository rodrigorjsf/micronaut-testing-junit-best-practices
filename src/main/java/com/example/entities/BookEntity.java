package com.example.entities;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Book entity.
 */
@Introspected
@Entity
@Table(name = "book", schema = "public")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private String title;

    @Min(1)
    @Column
    private int pages;

    @DateCreated
    @Column
    private LocalDateTime dateCreated;

    @NotNull
    @JoinColumn
    @ManyToOne(targetEntity =  AuthorEntity.class)
    private AuthorEntity author;

    public BookEntity(@NotBlank String title,
                      @Min(1) int pages,
                      @NotNull AuthorEntity author) {
        this.title = title;
        this.pages = pages;
        this.author = author;
    }

    public BookEntity(Long id, String title, int pages, LocalDateTime dateCreated, AuthorEntity author) {
        this.id = id;
        this.title = title;
        this.pages = pages;
        this.dateCreated = dateCreated;
        this.author = author;
    }

    public BookEntity() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public AuthorEntity getAuthor() {
        return author;
    }

    public void setAuthor(AuthorEntity author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "BookEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", pages=" + pages +
                ", dateCreated=" + dateCreated +
                ", author=" + author +
                '}';
    }
}
