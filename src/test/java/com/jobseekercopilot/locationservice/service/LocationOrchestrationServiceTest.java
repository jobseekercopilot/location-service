package com.jobseekercopilot.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobseekercopilot.locationservice.client.GoogleMapsGatewayClient;
import com.jobseekercopilot.locationservice.client.PostcodeGatewayClient;
import com.jobseekercopilot.locationservice.model.LocationContracts;
import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocationOrchestrationServiceTest {
    @Test
    void fullPostcodeAutocompleteUsesTheExactPostcodeLookup() {
        PostcodeGatewayClient postcodes = mock(PostcodeGatewayClient.class);
        when(postcodes.lookup("RG1 1AA")).thenReturn(new PostcodeGatewayClient.PostcodeResult(
                "RG1 1AA", "England", "South East", "Reading",
                new BigDecimal("51.4543"), new BigDecimal("-0.9781")));
        LocationOrchestrationService service = new LocationOrchestrationService(
                postcodes,
                mock(GoogleMapsGatewayClient.class),
                new SuggestionSessionStore(Duration.ofMinutes(5), 10),
                new CanonicalLocationFactory(),
                false,
                5);

        LocationContracts.AutocompleteResponse result = service.autocomplete(
                new LocationContracts.AutocompleteRequest("rg1 1aa", UUID.randomUUID().toString(), List.of("GB")));

        assertThat(result.suggestions()).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.primaryText()).isEqualTo("Reading, South East");
            assertThat(suggestion.secondaryText()).contains("RG1 1AA");
            assertThat(suggestion.precisionHint()).isEqualTo(LocationEnums.Precision.POSTCODE_CENTROID);
        });
        assertThat(result.attribution())
                .isEqualTo(LocationContracts.Attribution.postcodesIo());
        assertThat(result.source()).isEqualTo("POSTCODES_IO");
        verify(postcodes).lookup("RG1 1AA");
        verify(postcodes, never()).search("rg1 1aa", 5);
    }

    @Test
    void placeAutocompleteAndResolutionRequirePostcodesIoAttribution() {
        PostcodeGatewayClient postcodes = mock(PostcodeGatewayClient.class);
        when(postcodes.search("Leeds", 5)).thenReturn(List.of(new PostcodeGatewayClient.PlaceResult(
                "place-1", "Leeds", null, "Yorkshire and the Humber", "Leeds",
                new BigDecimal("53.797"), new BigDecimal("-1.548"))));
        when(postcodes.lookup("LS1 1AA")).thenReturn(new PostcodeGatewayClient.PostcodeResult(
                "LS1 1AA", "England", "Yorkshire and the Humber", "Leeds",
                new BigDecimal("53.797"), new BigDecimal("-1.548")));
        LocationOrchestrationService service = new LocationOrchestrationService(
                postcodes,
                mock(GoogleMapsGatewayClient.class),
                new SuggestionSessionStore(Duration.ofMinutes(5), 10),
                new CanonicalLocationFactory(),
                false,
                5);

        LocationContracts.AutocompleteResponse autocomplete = service.autocomplete(
                new LocationContracts.AutocompleteRequest(
                        "Leeds", UUID.randomUUID().toString(), List.of("GB")));
        assertThat(autocomplete.attribution())
                .isEqualTo(LocationContracts.Attribution.postcodesIo());

        String sessionId = autocomplete.sessionId();
        // The place suggestion does not carry a postcode; verify the exact-postcode
        // resolution path independently with a full-postcode suggestion.
        LocationContracts.AutocompleteResponse postcodeAutocomplete = service.autocomplete(
                new LocationContracts.AutocompleteRequest(
                        "LS1 1AA", sessionId, List.of("GB")));
        String suggestionId = postcodeAutocomplete.suggestions().get(0).suggestionId();
        LocationContracts.ResolveResponse resolved = service.resolve(
                new LocationContracts.ResolveRequest(sessionId, suggestionId));

        assertThat(resolved.resolutionStatus())
                .isEqualTo(LocationContracts.ResolutionStatus.RESOLVED);
        assertThat(resolved.attribution())
                .isEqualTo(LocationContracts.Attribution.postcodesIo());
    }
}
