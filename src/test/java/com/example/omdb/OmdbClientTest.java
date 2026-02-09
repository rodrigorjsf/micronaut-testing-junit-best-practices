package com.example.omdb;

import com.example.AbstractIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the real OMDB API client.
 * <p>
 * This test only runs when the {@code OMDB_API_KEY} environment variable is set to a valid key.
 * See <a href="http://www.omdbapi.com/">omdbapi.com</a> to generate one. It's free.
 * <p>
 * Also keep in mind that this test sends a real HTTP call to the 3rd party API.
 */
@EnabledIfEnvironmentVariable(named = "OMDB_API_KEY", matches = ".+")
class OmdbClientTest extends AbstractIntegrationTest {

    @Inject
    OmdbClient omdbClient;

    @Test
    void findMovieOnOmdb() {
        Optional<Movie> optMovie = omdbClient.findMovieByTitle("carrie");
        assertThat(optMovie).isPresent();

        Movie movie = optMovie.get();
        assertThat(movie.getTitle().toLowerCase()).isEqualTo("carrie");
        assertThat(movie.getYear()).isNotNull();
    }

    @Test
    void findNonExistentMovieReturnsEmpty() {
        Optional<Movie> optMovie = omdbClient.findMovieByTitle("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        assertThat(optMovie).isEmpty();
    }
}
