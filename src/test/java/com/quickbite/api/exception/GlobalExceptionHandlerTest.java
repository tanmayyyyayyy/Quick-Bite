package com.quickbite.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickbite.api.api.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourcesToStructured404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Order", 42), request("/api/orders/42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(body -> {
            assertThat(body.error()).isEqualTo("NOT_FOUND");
            assertThat(body.message()).contains("Order not found");
            assertThat(body.path()).isEqualTo("/api/orders/42");
            assertThat(body.timestamp()).isNotNull();
        });
    }

    @Test
    void mapsConflictsTo409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Duplicate email"), request("/api/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("Duplicate email");
    }

    @Test
    void mapsSemanticFailuresTo422() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnprocessable(
                new UnprocessableEntityException("Cart is empty"), request("/api/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error()).isEqualTo("UNPROCESSABLE_ENTITY");
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }
}
