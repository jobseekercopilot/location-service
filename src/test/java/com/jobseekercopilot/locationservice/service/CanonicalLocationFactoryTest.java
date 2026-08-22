package com.jobseekercopilot.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobseekercopilot.locationservice.client.PostcodeGatewayClient;
import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CanonicalLocationFactoryTest {
    @Test
    void postcodeCoordinatesAreExplicitlyCentroidsWithPostcodesIoProvenance() {
        var result = new CanonicalLocationFactory().fromPostcode(
                new PostcodeGatewayClient.PostcodeResult(
                        "ub3 1aa", "England", "London", "Hillingdon",
                        new BigDecimal("51.50"), new BigDecimal("-0.42")),
                "UB3 1AA");

        assertThat(result.postcode()).isEqualTo("UB3 1AA");
        assertThat(result.precision()).isEqualTo(LocationEnums.Precision.POSTCODE_CENTROID);
        assertThat(result.providerReferences()).extracting(CanonicalLocationFactoryTest::providerName)
                .containsExactly("POSTCODES_IO");
    }

    private static String providerName(com.jobseekercopilot.locationservice.model.CanonicalLocation.ProviderReference value) {
        return value.provider().name();
    }
}
