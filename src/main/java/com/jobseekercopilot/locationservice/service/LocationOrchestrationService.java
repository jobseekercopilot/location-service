package com.jobseekercopilot.locationservice.service;

import com.jobseekercopilot.locationservice.client.GoogleMapsGatewayClient;
import com.jobseekercopilot.locationservice.client.PostcodeGatewayClient;
import com.jobseekercopilot.locationservice.model.CanonicalLocation;
import com.jobseekercopilot.locationservice.model.LocationContracts;
import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocationOrchestrationService {
    private final PostcodeGatewayClient postcodeClient;
    private final GoogleMapsGatewayClient googleClient;
    private final SuggestionSessionStore sessions;
    private final CanonicalLocationFactory canonicalLocations;
    private final boolean googleEnabled;
    private final int maximumResults;

    public LocationOrchestrationService(
            PostcodeGatewayClient postcodeClient,
            GoogleMapsGatewayClient googleClient,
            SuggestionSessionStore sessions,
            CanonicalLocationFactory canonicalLocations,
            @Value("${google.maps.enabled:false}") boolean googleEnabled,
            @Value("${location.search.maximum-results:5}") int maximumResults) {
        this.postcodeClient = postcodeClient;
        this.googleClient = googleClient;
        this.sessions = sessions;
        this.canonicalLocations = canonicalLocations;
        this.googleEnabled = googleEnabled;
        this.maximumResults = maximumResults;
    }

    public LocationContracts.AutocompleteResponse autocomplete(LocationContracts.AutocompleteRequest request) {
        String input = canonicalInput(request.input());
        String sessionId = validSession(request.sessionId());
        List<String> countries = request.countryCodes() == null || request.countryCodes().isEmpty()
                ? List.of("GB") : request.countryCodes();
        if (countries.stream().anyMatch(country -> !"GB".equalsIgnoreCase(country))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only GB locations are supported");
        }
        if (googleEnabled) {
            GoogleMapsGatewayClient.GoogleAutocompleteResponse google =
                    googleClient.autocomplete(input, sessionId, List.of("GB"));
            List<GoogleMapsGatewayClient.GoogleSuggestion> providerSuggestions =
                    google == null || google.suggestions() == null ? List.of() : google.suggestions();
            List<LocationContracts.Suggestion> suggestions = providerSuggestions.stream()
                    .limit(maximumResults)
                    .map(value -> new LocationContracts.Suggestion(
                            sessions.put(sessionId, LocationEnums.Provider.GOOGLE_PLACES,
                                    value.providerReference(), null, value.primaryText()),
                            value.primaryText(), value.secondaryText(), value.precisionHint()))
                    .toList();
            return new LocationContracts.AutocompleteResponse(
                    sessionId, suggestions, LocationContracts.Attribution.google(), "GOOGLE_PLACES");
        }
        return postcodeAutocomplete(input, sessionId);
    }

    public LocationContracts.AutocompleteResponse postcodeAutocomplete(String input, String sessionId) {
        if (isFullPostcode(input)) {
            String postcode = canonicalPostcode(input);
            CanonicalLocation location = canonicalLocations.fromPostcode(postcodeClient.lookup(postcode), postcode);
            LocationContracts.Suggestion suggestion = new LocationContracts.Suggestion(
                    sessions.put(sessionId, LocationEnums.Provider.POSTCODES_IO,
                            postcode, location.postcode(), location.displayName()),
                    location.displayName(), secondary(location.postcode(), location.region()),
                    LocationEnums.Precision.POSTCODE_CENTROID);
            return new LocationContracts.AutocompleteResponse(
                    sessionId, List.of(suggestion), LocationContracts.Attribution.none(), "POSTCODES_IO");
        }
        List<LocationContracts.Suggestion> suggestions = postcodeClient.search(input, maximumResults).stream()
                .map(value -> new LocationContracts.Suggestion(
                        sessions.put(sessionId, LocationEnums.Provider.POSTCODES_IO,
                                value.id(), value.postcode(), value.name()),
                        value.name(), secondary(value.postcode(), value.region()),
                        LocationEnums.Precision.LOCALITY_CENTROID))
                .toList();
        return new LocationContracts.AutocompleteResponse(
                sessionId, suggestions, LocationContracts.Attribution.none(), "POSTCODES_IO");
    }

    public LocationContracts.ResolveResponse resolve(LocationContracts.ResolveRequest request) {
        SuggestionSessionStore.Entry selection = sessions.take(request.sessionId(), request.suggestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion expired or unknown"));
        if (selection.provider() == LocationEnums.Provider.POSTCODES_IO) {
            CanonicalLocation location = lookupPostcode(selection.postcode());
            return new LocationContracts.ResolveResponse(
                    location, LocationContracts.ResolutionStatus.RESOLVED,
                    LocationContracts.Attribution.none(), null);
        }
        GoogleMapsGatewayClient.GooglePlaceDetails details =
                googleClient.resolve(selection.providerReference(), request.sessionId());
        if (details.postcode() == null || details.postcode().isBlank()) {
            return new LocationContracts.ResolveResponse(
                    canonicalLocations.unresolvedGoogleReference(
                            selection.providerReference(), selection.displayText()),
                    LocationContracts.ResolutionStatus.CONFIRMATION_REQUIRED,
                    LocationContracts.Attribution.google(), "POSTCODE_REQUIRED");
        }
        CanonicalLocation postcodeLocation = lookupPostcode(details.postcode());
        CanonicalLocation resolved = withGoogleReference(postcodeLocation, details.providerReference());
        return new LocationContracts.ResolveResponse(
                resolved, LocationContracts.ResolutionStatus.RESOLVED,
                LocationContracts.Attribution.google(), null);
    }

    public CanonicalLocation lookupPostcode(String postcode) {
        String clean = canonicalPostcode(postcode);
        return canonicalLocations.fromPostcode(postcodeClient.lookup(clean), clean);
    }

    public List<LocationContracts.LegacyLocation> legacySearch(String query) {
        return postcodeClient.search(canonicalInput(query), 10).stream()
                .map(value -> new LocationContracts.LegacyLocation(
                        value.id(), value.name(), value.postcode(), value.region(),
                        decimal(value.latitude()), decimal(value.longitude())))
                .toList();
    }

    public List<PostcodeGatewayClient.PlaceResult> compatibilityPlaces(String query, int limit) {
        if (limit < 1 || limit > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location result limit must be between 1 and 10");
        }
        return postcodeClient.search(canonicalLegacyInput(query), limit);
    }

    public PostcodeGatewayClient.PostcodeResult compatibilityPostcode(String postcode) {
        return postcodeClient.lookup(canonicalPostcode(postcode));
    }

    public LocationContracts.LegacyLocation legacyLookup(String postcode) {
        CanonicalLocation value = lookupPostcode(postcode);
        return new LocationContracts.LegacyLocation(
                value.postcode(), value.displayName(), value.postcode(), value.region(),
                decimal(value.latitude()), decimal(value.longitude()));
    }

    private CanonicalLocation withGoogleReference(CanonicalLocation source, String placeId) {
        List<CanonicalLocation.ProviderReference> references = new java.util.ArrayList<>(source.providerReferences());
        references.add(new CanonicalLocation.ProviderReference(
                LocationEnums.Provider.GOOGLE_PLACES, placeId, Instant.now()));
        return new CanonicalLocation(
                source.locationId(), source.displayName(), source.countryCode(), source.postcode(),
                source.locality(), source.region(), source.latitude(), source.longitude(),
                source.locationType(), source.precision(), source.confidence(), List.copyOf(references),
                source.fieldProvenance(), source.normalisedAt());
    }

    private String validSession(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sessionId");
        }
    }

    private String canonicalInput(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() < 3 || clean.length() > 200 || clean.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location input must contain 3 to 200 characters");
        }
        return clean;
    }

    private String canonicalLegacyInput(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() < 2 || clean.length() > 80 || clean.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid place search query");
        }
        return clean;
    }

    private String canonicalPostcode(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.UK);
        if (clean.length() < 2 || clean.length() > 8 || !clean.matches("[A-Z0-9 ]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UK postcode or outcode");
        }
        return clean;
    }

    private boolean isFullPostcode(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.UK);
        return clean.matches("(?:GIR 0AA|[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2})");
    }

    private String secondary(String postcode, String region) {
        if (postcode == null || postcode.isBlank()) {
            return region;
        }
        return region == null || region.isBlank() ? postcode : postcode + " · " + region;
    }

    private Double decimal(java.math.BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
