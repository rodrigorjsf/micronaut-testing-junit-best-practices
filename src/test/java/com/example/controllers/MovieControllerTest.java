package com.example.controllers;

import com.example.AbstractServerTest;
import com.example.omdb.Movie;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the movie controller using a mock OMDB API server.
 * <p>
 * A second embedded server is started to serve as the OMDB API mock.
 * The main application's {@code omdb.base-url} is configured to point to it.
 * </p>
 */
class MovieControllerTest extends AbstractServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MovieControllerTest.class);

    private final int omdbPort = SocketUtils.findAvailableTcpPort();
    private EmbeddedServer omdbServer;

    @Override
    @NonNull
    public Map<String, String> getProperties() {
        Map<String, String> props = super.getProperties();
        props.put("omdb.base-url", "http://localhost:" + omdbPort);
        return props;
    }

    @BeforeAll
    void startOmdbMock() {
        Map<String, Object> config = new HashMap<>();
        config.put("micronaut.server.port", omdbPort);
        config.put("spec.name", "MovieControllerTest");
        config.put("datasources.default.db-type", "postgres");
        config.put("datasources.default.dialect", "POSTGRES");
        config.put("datasources.default.driver-class-name", "org.postgresql.Driver");
        config.put("datasources.default.schema-generate", "NONE");
        config.put("mockSecurityService", "true");
        omdbServer = ApplicationContext.run(EmbeddedServer.class, config);
    }

    @AfterAll
    void stopOmdbMock() {
        if (omdbServer != null) {
            omdbServer.close();
        }
    }

    @Test
    void findMovieByTitle() {
        URI uri = UriBuilder.of("/movies/by-title")
                .queryParam("title", "it does not really matter")
                .build();

        HttpResponse<Movie> response = getClient().exchange(HttpRequest.GET(uri), Movie.class);
        assertEquals(HttpStatus.OK, response.status());

        Movie movie = response.body();
        assertThat(movie).isNotNull();
        assertThat(movie.getTitle()).isEqualTo("Star Wars: Episode IV - A New Hope");
        assertThat(movie.getYear()).isEqualTo("1977");
    }

    @Controller("/")
    @Requires(property = "spec.name", value = "MovieControllerTest")
    static class OmdbMock {

        @Get
        String findMovie(@QueryValue("t") String title) {
            LOG.debug(" ============== {} ============== ", title);
            // Real response from the OMDB API, returned as a String so Micronaut handles the marshalling
            return """
                    {"Title":"Star Wars: Episode IV - A New Hope","Year":"1977","Rated":"PG","Released":"25 May 1977","Runtime":"121 min","Genre":"Action, Adventure, Fantasy, Sci-Fi","Director":"George Lucas","Writer":"George Lucas","Actors":"Mark Hamill, Harrison Ford, Carrie Fisher, Peter Cushing","Plot":"Luke Skywalker joins forces with a Jedi Knight, a cocky pilot, a Wookiee and two droids to save the galaxy from the Empire's world-destroying battle station, while also attempting to rescue Princess Leia from the mysterious Darth Vader.","Language":"English","Country":"USA","Awards":"Won 6 Oscars. Another 52 wins & 28 nominations.","Poster":"https://m.media-amazon.com/images/M/MV5BNzVlY2MwMjktM2E4OS00Y2Y3LWE3ZjctYzhkZGM3YzA1ZWM2XkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg","Ratings":[{"Source":"Internet Movie Database","Value":"8.6/10"},{"Source":"Rotten Tomatoes","Value":"92%"},{"Source":"Metacritic","Value":"90/100"}],"Metascore":"90","imdbRating":"8.6","imdbVotes":"1,194,693","imdbID":"tt0076759","Type":"movie","DVD":"21 Sep 2004","BoxOffice":"N/A","Production":"20th Century Fox","Website":"N/A","Response":"True"}""";
        }
    }
}
