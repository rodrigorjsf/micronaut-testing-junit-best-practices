package com.example.openapi;

import com.example.AbstractServerTest;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiTest extends AbstractServerTest {

    @Test
    void openApiYmlFileIsExposed() {
        HttpResponse<?> response = getClient().exchange(HttpRequest.GET("/swagger/demo-0.1.yml"));

        assertEquals(HttpStatus.OK, response.status());
    }
}
