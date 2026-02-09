package com.example.controllers;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Serdeable
public class CreateAuthorRequest {

    @NotBlank
    @Schema(required = true, description = "The author name", example = "Stephen King")
    private final String name;

    public CreateAuthorRequest(@NotBlank String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
