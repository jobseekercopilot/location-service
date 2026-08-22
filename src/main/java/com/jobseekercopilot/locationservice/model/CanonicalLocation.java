package com.jobseekercopilot.locationservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CanonicalLocation(
        UUID locationId,
        String displayName,
        String countryCode,
        String postcode,
        String locality,
        String region,
        BigDecimal latitude,
        BigDecimal longitude,
        LocationEnums.LocationType locationType,
        LocationEnums.Precision precision,
        LocationEnums.Confidence confidence,
        List<ProviderReference> providerReferences,
        List<FieldProvenance> fieldProvenance,
        Instant normalisedAt) {

    public record ProviderReference(LocationEnums.Provider provider, String externalId, Instant observedAt) {
    }

    public record FieldProvenance(
            String field,
            LocationEnums.Provider source,
            LocationEnums.ProvenanceMethod method,
            Instant observedAt,
            LocationEnums.Confidence confidence) {
    }
}
