package com.jobseekercopilot.locationservice.client;

import com.jobseekercopilot.generated.googlemapsgateway.api.PlacesApi;
import com.jobseekercopilot.generated.googlemapsgateway.api.RoutesApi;
import com.jobseekercopilot.generated.googlemapsgateway.client.ApiClient;
import com.jobseekercopilot.generated.googlemapsgateway.model.Precision;
import com.jobseekercopilot.generated.googlemapsgateway.model.TravelMode;
import com.jobseekercopilot.locationservice.model.CommuteContracts;
import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleMapsGatewayClient {
    private final PlacesApi places;
    private final RoutesApi routes;

    public GoogleMapsGatewayClient(
            RestTemplateBuilder builder,
            @Value("${google.maps.gateway.url}") String baseUrl,
            @Value("${google.maps.gateway.service-token}") String serviceToken) {
        if (serviceToken == null || serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("GOOGLE_MAPS_GATEWAY_TOKEN must contain at least 32 bytes");
        }
        ApiClient client = new ApiClient(builder.build()).setBasePath(baseUrl);
        client.setApiKey(serviceToken);
        this.places = new PlacesApi(client);
        this.routes = new RoutesApi(client);
    }

    public GoogleAutocompleteResponse autocomplete(String input, String sessionId, List<String> countryCodes) {
        var response = places.autocompletePlaces(
                new com.jobseekercopilot.generated.googlemapsgateway.model.AutocompleteRequest()
                        .input(input)
                        .sessionId(UUID.fromString(sessionId))
                        .countryCodes(countryCodes.stream()
                                .map(com.jobseekercopilot.generated.googlemapsgateway.model.AutocompleteRequest
                                        .CountryCodesEnum::fromValue)
                                .toList()));
        return new GoogleAutocompleteResponse(
                response.getSessionId().toString(),
                response.getSuggestions() == null ? List.of() : response.getSuggestions().stream()
                        .map(value -> new GoogleSuggestion(
                                value.getProviderReference(),
                                value.getPrimaryText(),
                                value.getSecondaryText(),
                                LocationEnums.Precision.valueOf(value.getPrecisionHint().getValue())))
                        .toList());
    }

    public GooglePlaceDetails resolve(String placeId, String sessionId) {
        var response = places.resolvePlace(
                new com.jobseekercopilot.generated.googlemapsgateway.model.ResolveRequest()
                        .providerReference(placeId)
                        .sessionId(UUID.fromString(sessionId)));
        return new GooglePlaceDetails(
                response.getProviderReference(),
                response.getDisplayText(),
                response.getPostcode(),
                response.getLocality(),
                response.getRegion(),
                response.getCountryCode(),
                decimal(response.getLatitude()),
                decimal(response.getLongitude()),
                instant(response.getObservedAt()));
    }

    public CommuteContracts.CommuteMatrixResponse matrix(CommuteContracts.CommuteMatrixRequest request) {
        var response = routes.computeRouteMatrix(
                new com.jobseekercopilot.generated.googlemapsgateway.model.CommuteMatrixRequest()
                        .origin(point(request.origin()))
                        .destinations(request.destinations().stream().map(this::point).toList())
                        .modes(request.modes().stream()
                                .map(mode -> TravelMode.fromValue(mode.name()))
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                        .departureTime(OffsetDateTime.ofInstant(request.departureTime(), ZoneOffset.UTC)));
        return new CommuteContracts.CommuteMatrixResponse(
                response == null || response.getEstimates() == null ? List.of()
                        : response.getEstimates().stream().map(value -> new CommuteContracts.CommuteEstimate(
                                value.getDestinationReferenceId(),
                                LocationEnums.TravelMode.valueOf(value.getMode().getValue()),
                                CommuteContracts.EstimateStatus.valueOf(value.getStatus().getValue()),
                                value.getDurationMinutes(),
                                decimal(value.getDistanceMiles()),
                                instant(value.getCalculatedFor()),
                                value.getReasonCode(),
                                value.getProviderAttribution() == null
                                        ? null : value.getProviderAttribution().getValue())).toList());
    }

    public record GoogleAutocompleteResponse(String sessionId, List<GoogleSuggestion> suggestions) { }

    public record GoogleSuggestion(
            String providerReference,
            String primaryText,
            String secondaryText,
            LocationEnums.Precision precisionHint) { }

    public record GooglePlaceDetails(
            String providerReference,
            String displayText,
            String postcode,
            String locality,
            String region,
            String countryCode,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant observedAt) { }

    private com.jobseekercopilot.generated.googlemapsgateway.model.RoutePoint point(
            CommuteContracts.RoutePoint value) {
        return new com.jobseekercopilot.generated.googlemapsgateway.model.RoutePoint()
                .referenceId(value.referenceId())
                .latitude(value.latitude().doubleValue())
                .longitude(value.longitude().doubleValue())
                .precision(Precision.fromValue(value.precision().name()));
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
