package com.example.services;

import com.example.AbstractIntegrationTest;
import com.example.entities.AuthorEntity;
import com.example.fixtures.AuthorFixture;
import com.example.fixtures.BookFixture;
import com.example.model.Author;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorServiceTest extends AbstractIntegrationTest implements AuthorFixture, BookFixture {

    @Inject
    AuthorService authorService;

    @Test
    void saveAnAuthor() {
        String name = "Stephen King";

        Author author = authorService.saveAuthor(name);

        assertThat(author.getId()).isNotNull();
        assertThat(author.getName()).isEqualTo(name);

        authorRepository.deleteAll();
    }

    @Test
    void addBookToExistingAuthor() {
        AuthorEntity authorEntity = saveAuthor();
        SaveBook saveBook = createSaveBook(authorEntity.getId());

        authorService.addBookToAuthor(saveBook);

        Author author = authorRepository.findAuthorByName(authorEntity.getName()).orElseThrow();
        assertThat(author.getBooks()).hasSize(1);
        assertThat(author.getBooks().get(0).getTitle()).isEqualTo(saveBook.getTitle());
        assertThat(author.getBooks().get(0).getPages()).isEqualTo(saveBook.getPages());

        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
