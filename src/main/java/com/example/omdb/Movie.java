package com.example.omdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OMDB Movie.
 * <p>
 * See http://www.omdbapi.com/#examples
 */
@Serdeable
public class Movie {

    // Only interested in these two fields
    @JsonProperty("Title")
    @Schema(required = true, description = "The movie title", example = "Carrie")
    private String title;

    @JsonProperty("Year")
    @Schema(required = true, description = "The movie year", example = "1977")
    private String year;

    public Movie() {
    }

    public Movie(String title, String year) {
        this.title = title;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", year='" + year + '\'' +
                '}';
    }
}
