package com.jobseekercopilot.locationservice.service;

import com.jobseekercopilot.locationservice.model.LocationEnums;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SuggestionSessionStore {
    private final Duration ttl;
    private final int maximumEntries;
    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Autowired
    public SuggestionSessionStore(
            @Value("${location.session.ttl:10m}") Duration ttl,
            @Value("${location.session.maximum-entries:10000}") int maximumEntries) {
        this(ttl, maximumEntries, Clock.systemUTC());
    }

    SuggestionSessionStore(Duration ttl, int maximumEntries, Clock clock) {
        this.ttl = ttl;
        this.maximumEntries = maximumEntries;
        this.clock = clock;
    }

    public String put(
            String sessionId,
            LocationEnums.Provider provider,
            String providerReference,
            String postcode,
            String displayText) {
        evictExpired();
        if (entries.size() >= maximumEntries) {
            throw new IllegalStateException("Location suggestion capacity exceeded");
        }
        String suggestionId = UUID.randomUUID().toString();
        entries.put(key(sessionId, suggestionId),
                new Entry(provider, providerReference, postcode, displayText, clock.instant().plus(ttl)));
        return suggestionId;
    }

    public Optional<Entry> take(String sessionId, String suggestionId) {
        Entry entry = entries.remove(key(sessionId, suggestionId));
        if (entry == null || !entry.expiresAt().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private void evictExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String key(String sessionId, String suggestionId) {
        return sessionId + ':' + suggestionId;
    }

    public record Entry(
            LocationEnums.Provider provider,
            String providerReference,
            String postcode,
            String displayText,
            Instant expiresAt) { }
}
