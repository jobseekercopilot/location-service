package com.jobseekercopilot.locationservice.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SuggestionSessionConfiguration {
    @Bean
    SuggestionSessionStore suggestionSessionStore(
            @Value("${location.session.ttl:10m}") Duration ttl,
            @Value("${location.session.maximum-entries:10000}") int maximumEntries) {
        return new SuggestionSessionStore(ttl, maximumEntries);
    }
}
