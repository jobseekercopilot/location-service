package com.jobseekercopilot.locationservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class LocationContracts {
    private LocationContracts() {
    }

    public record AutocompleteRequest(
            @NotBlank @Size(min = 3, max = 200) String input,
            String sessionId,
            List<String> countryCodes) {
    }

    public record AutocompleteResponse(
            String sessionId,
            List<Suggestion> suggestions,
            Attribution attribution,
            String source) {
    }

    public record Suggestion(
            String suggestionId,
            String primaryText,
            String secondaryText,
            LocationEnums.Precision precisionHint) {
    }

    public record Attribution(boolean required, String provider) {
        public static Attribution google() {
            return new Attribution(true, "GOOGLE_MAPS");
        }

        public static Attribution none() {
            return new Attribution(false, null);
        }
    }

    public record ResolveRequest(@NotBlank String sessionId, @NotBlank String suggestionId) {
    }

    public record ResolveResponse(
            CanonicalLocation location,
            ResolutionStatus resolutionStatus,
            Attribution attribution,
            String reasonCode) {
    }

    public enum ResolutionStatus { RESOLVED, CONFIRMATION_REQUIRED }

    public record LegacyLocation(
            String id,
            String name,
            String postcode,
            String region,
            Double latitude,
            Double longitude) {
    }
}
