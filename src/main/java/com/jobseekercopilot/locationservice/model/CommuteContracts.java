package com.jobseekercopilot.locationservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CommuteContracts {
    private CommuteContracts() {
    }

    public record CommuteMatrixRequest(
            @NotNull @Valid RoutePoint origin,
            @NotEmpty @Size(max = 5) List<@Valid RoutePoint> destinations,
            @NotEmpty List<LocationEnums.TravelMode> modes,
            @NotNull Instant departureTime) {
    }

    public record RoutePoint(
            @NotNull String referenceId,
            @NotNull BigDecimal latitude,
            @NotNull BigDecimal longitude,
            @NotNull LocationEnums.Precision precision) {
    }

    public record CommuteMatrixResponse(List<CommuteEstimate> estimates) {
    }

    public record CommuteEstimate(
            String destinationReferenceId,
            LocationEnums.TravelMode mode,
            EstimateStatus status,
            Integer durationMinutes,
            BigDecimal distanceMiles,
            Instant calculatedFor,
            String reasonCode,
            String providerAttribution) {
    }

    public enum EstimateStatus { ESTIMATED, APPROXIMATE, UNAVAILABLE }
}
