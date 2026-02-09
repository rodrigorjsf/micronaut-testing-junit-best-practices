package com.example.repositories;

import com.example.AbstractIntegrationTest;
import com.example.entities.AuthorEntity;
import com.example.entities.BookEntity;
import com.example.fixtures.AuthorFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookRepositoryTest extends AbstractIntegrationTest implements AuthorFixture {

    @Test
    void saveABook() {
        AuthorEntity author = saveAuthor("Stephen King");

        BookEntity book = new BookEntity("The Stand", 550, author);
        bookRepository.save(book);

        assertThat(book.getId()).isNotNull();
        assertThat(book.getDateCreated()).isNotNull();

        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
