package com.jobseekercopilot.locationservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void preservesRedactedUnsupportedPostcodeCoverageResponse() {
        ResponseEntity<Map<String, Object>> response = handler.status(
                new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "This postcode area is not currently supported."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody())
                .containsEntry("status", 422)
                .containsEntry("message", "This postcode area is not currently supported.");
        assertThat(response.getBody().toString()).doesNotContain("BT", "provider");
    }

    @Test
    void transportAndMalformedProviderFailuresStayGeneric() {
        assertThat(handler.providerUnavailable().getBody())
                .containsEntry("message", "Location service is temporarily unavailable.");
        assertThat(handler.invalidProviderResponse().getBody())
                .containsEntry("message", "Location provider returned an invalid response.");
    }
}
