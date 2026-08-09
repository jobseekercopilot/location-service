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
        verify(postcodes).lookup("RG1 1AA");
        verify(postcodes, never()).search("rg1 1aa", 5);
    }
}
