package com.jobseekercopilot.locationservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PostcodeGatewayClient {
    private final RestClient client;

    public PostcodeGatewayClient(RestClient.Builder builder, @Value("${postcode.gateway.url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public List<PlaceResult> search(String query, int limit) {
        List<PlaceResult> result = client.get()
                .uri(uri -> uri.path("/api/places").queryParam("q", query).queryParam("limit", limit).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw providerFailure(response.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() { });
        return result == null ? List.of() : List.copyOf(result);
    }

    public PostcodeResult lookup(String postcode) {
        return client.get()
                .uri("/api/postcodes/{postcode}", postcode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw providerFailure(response.getStatusCode());
                })
                .body(PostcodeResult.class);
    }

    private static ResponseStatusException providerFailure(HttpStatusCode statusCode) {
        HttpStatus status = switch (statusCode.value()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
        String message = switch (status) {
            case BAD_REQUEST -> "Invalid location request";
            case NOT_FOUND -> "Location not found";
            case UNPROCESSABLE_ENTITY -> "This postcode area is not currently supported.";
            case TOO_MANY_REQUESTS -> "Too many location requests. Try again later.";
            case SERVICE_UNAVAILABLE -> "Location service is temporarily unavailable.";
            case GATEWAY_TIMEOUT -> "Location service timed out.";
            default -> "Location provider returned an invalid response.";
        };
        return new ResponseStatusException(status, message);
    }

    public record PlaceResult(
            String id,
            String name,
            String postcode,
            String region,
            String adminDistrict,
            BigDecimal latitude,
            BigDecimal longitude) {
    }

    public record PostcodeResult(
            String postcode,
            String country,
            String region,
            @JsonProperty("admin_district") String adminDistrict,
            BigDecimal latitude,
            BigDecimal longitude) {
    }
}
