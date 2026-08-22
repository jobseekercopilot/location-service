package com.jobseekercopilot.locationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SuggestionSessionStoreTest {
    @Test
    void suggestionCanOnlyBeConsumedOnce() {
        SuggestionSessionStore store = new SuggestionSessionStore(
                Duration.ofMinutes(10), 10, Clock.fixed(Instant.parse("2026-08-09T08:00:00Z"), ZoneOffset.UTC));
        String id = store.put("session", LocationEnums.Provider.POSTCODES_IO, "place", "UB3", "Hayes");

        assertThat(store.take("session", id)).isPresent();
        assertThat(store.take("session", id)).isEmpty();
    }
}
