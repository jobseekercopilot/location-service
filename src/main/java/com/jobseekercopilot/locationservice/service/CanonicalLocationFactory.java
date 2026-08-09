package com.jobseekercopilot.locationservice.service;

import com.jobseekercopilot.locationservice.client.PostcodeGatewayClient;
import com.jobseekercopilot.locationservice.model.CanonicalLocation;
import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CanonicalLocationFactory {
    private final Clock clock = Clock.systemUTC();

    public CanonicalLocation fromPostcode(PostcodeGatewayClient.PostcodeResult source, String externalId) {
        if (source == null) {
            throw new IllegalArgumentException("Postcode result is required");
        }
        Instant observedAt = clock.instant();
        String postcode = normalisePostcode(source.postcode());
        String displayName = displayName(source.adminDistrict(), source.region(), postcode);
        List<CanonicalLocation.FieldProvenance> provenance = new ArrayList<>();
        add(provenance, "POSTCODE", observedAt);
        add(provenance, "REGION", observedAt);
        add(provenance, "COORDINATES", observedAt);
        add(provenance, "DISPLAY_NAME", observedAt);
        return new CanonicalLocation(
                UUID.randomUUID(), displayName, "GB", postcode, source.adminDistrict(), source.region(),
                source.latitude(), source.longitude(), LocationEnums.LocationType.POSTCODE,
                LocationEnums.Precision.POSTCODE_CENTROID, LocationEnums.Confidence.VERIFIED,
                List.of(new CanonicalLocation.ProviderReference(
                        LocationEnums.Provider.POSTCODES_IO, externalId == null ? postcode : externalId, observedAt)),
                List.copyOf(provenance), observedAt);
    }

    public CanonicalLocation fromPlace(PostcodeGatewayClient.PlaceResult source) {
        Instant observedAt = clock.instant();
        return new CanonicalLocation(
                UUID.randomUUID(), source.name(), "GB", normalisePostcode(source.postcode()), source.name(),
                source.region(), source.latitude(), source.longitude(), LocationEnums.LocationType.LOCALITY,
                LocationEnums.Precision.LOCALITY_CENTROID, LocationEnums.Confidence.VERIFIED,
                List.of(new CanonicalLocation.ProviderReference(
                        LocationEnums.Provider.POSTCODES_IO, source.id(), observedAt)),
                List.of(
                        field("DISPLAY_NAME", observedAt), field("POSTCODE", observedAt),
                        field("REGION", observedAt), field("COORDINATES", observedAt)),
                observedAt);
    }

    public CanonicalLocation unresolvedGoogleReference(String placeId, String displayText) {
        Instant observedAt = clock.instant();
        return new CanonicalLocation(
                UUID.randomUUID(), displayText, "GB", null, null, null, null, null,
                LocationEnums.LocationType.LOCALITY, LocationEnums.Precision.NONE,
                LocationEnums.Confidence.AMBIGUOUS,
                List.of(new CanonicalLocation.ProviderReference(
                        LocationEnums.Provider.GOOGLE_PLACES, placeId, observedAt)),
                List.of(), observedAt);
    }

    private CanonicalLocation.FieldProvenance field(String name, Instant at) {
        return new CanonicalLocation.FieldProvenance(name, LocationEnums.Provider.POSTCODES_IO,
                LocationEnums.ProvenanceMethod.LOOKUP_VERIFIED, at, LocationEnums.Confidence.VERIFIED);
    }

    private void add(List<CanonicalLocation.FieldProvenance> fields, String name, Instant at) {
        fields.add(field(name, at));
    }

    private String displayName(String district, String region, String postcode) {
        if (district != null && !district.isBlank() && region != null && !region.isBlank()) {
            return district + ", " + region;
        }
        if (district != null && !district.isBlank()) {
            return district;
        }
        if (region != null && !region.isBlank()) {
            return region;
        }
        return postcode;
    }

    private String normalisePostcode(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.UK);
    }
}
