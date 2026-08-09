package com.jobseekercopilot.locationservice.model;

public final class LocationEnums {
    private LocationEnums() {
    }

    public enum LocationType { POSTCODE, LOCALITY, ADDRESS, WORKPLACE, REMOTE, UNKNOWN }
    public enum Precision { EXACT_ADDRESS, POSTCODE_CENTROID, LOCALITY_CENTROID, PROVIDER_COORDINATE, NONE }
    public enum Confidence { VERIFIED, PROVIDED, INFERRED, AMBIGUOUS, UNKNOWN }
    public enum Provider { GOOGLE_PLACES, POSTCODES_IO, JOB_PROVIDER, USER, LEGACY }
    public enum ProvenanceMethod { DIRECT, USER_CONFIRMED, LOOKUP_VERIFIED, INFERRED }
    public enum TravelMode { DRIVE, TRANSIT }
}
