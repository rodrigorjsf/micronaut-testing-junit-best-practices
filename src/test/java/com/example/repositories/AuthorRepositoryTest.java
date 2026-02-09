package com.example.repositories;

import com.example.AbstractIntegrationTest;
import com.example.entities.AuthorEntity;
import com.example.fixtures.AuthorFixture;
import com.example.fixtures.BookFixture;
import com.example.model.Author;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorRepositoryTest extends AbstractIntegrationTest implements AuthorFixture, BookFixture {

    @Test
    void saveAnAuthor() {
        AuthorEntity author = new AuthorEntity("Stephen King");
        authorRepository.save(author);

        assertThat(author.getId()).isNotNull();
        assertThat(author.getDateCreated()).isNotNull();

        authorRepository.deleteAll();
    }

    @Test
    void findAuthorByNameWithTheirBooks() {
        String name = "Stephen King";
        AuthorEntity authorEntity = saveAuthor(name);

        saveBook("book1", authorEntity);
        saveBook("book2", authorEntity);
        saveBook("book3", authorEntity);

        Optional<Author> optAuthor = authorRepository.findAuthorByName(name);
        assertThat(optAuthor).isPresent();

        Author author = optAuthor.get();
        assertThat(author.getId()).isEqualTo(authorEntity.getId());
        assertThat(author.getName()).isEqualTo(name);
        assertThat(author.getBooks()).hasSize(3);

        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
