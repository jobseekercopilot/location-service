package com.jobseekercopilot.locationservice.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                .body(new ParameterizedTypeReference<>() { });
        return result == null ? List.of() : List.copyOf(result);
    }

    public PostcodeResult lookup(String postcode) {
        return client.get()
                .uri("/api/postcodes/{postcode}", postcode)
                .retrieve()
                .body(PostcodeResult.class);
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
            String adminDistrict,
            BigDecimal latitude,
            BigDecimal longitude) {
    }
}
