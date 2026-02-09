package com.example.fixtures;

import com.example.entities.AuthorEntity;
import com.example.entities.BookEntity;
import com.example.repositories.BookRepository;
import com.example.services.SaveBook;

/**
 * Test fixture for creating {@link BookEntity} and {@link SaveBook} instances.
 * <p>
 * Implement this interface in test classes that extend {@code AbstractIntegrationTest}
 * (which provides {@link #getBookRepository()}).
 * </p>
 */
public interface BookFixture {

    BookRepository getBookRepository();

    default BookEntity saveBook(String title, AuthorEntity author, int pages) {
        BookEntity bookEntity = new BookEntity(title, pages, author);
        return getBookRepository().save(bookEntity);
    }

    default BookEntity saveBook(String title, AuthorEntity author) {
        return saveBook(title, author, 1);
    }

    default SaveBook createSaveBook(Long authorId, String title, int pages) {
        return new SaveBook(title, pages, authorId);
    }

    default SaveBook createSaveBook(Long authorId) {
        return createSaveBook(authorId, "book title", 100);
    }
}
