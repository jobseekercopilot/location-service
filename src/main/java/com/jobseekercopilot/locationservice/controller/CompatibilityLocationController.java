package com.jobseekercopilot.locationservice.controller;

import com.jobseekercopilot.locationservice.client.PostcodeGatewayClient;
import com.jobseekercopilot.locationservice.service.LocationOrchestrationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary v1 compatibility surface used while location-gateway is thinned.
 * New consumers must use the internal v1 domain API instead.
 */
@RestController
@RequestMapping("/api")
public class CompatibilityLocationController {
    private final LocationOrchestrationService locations;

    public CompatibilityLocationController(LocationOrchestrationService locations) {
        this.locations = locations;
    }

    @GetMapping("/places")
    public List<PostcodeGatewayClient.PlaceResult> places(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return locations.compatibilityPlaces(query, limit);
    }

    @GetMapping("/postcodes/{postcode}")
    public PostcodeGatewayClient.PostcodeResult postcode(@PathVariable String postcode) {
        return locations.compatibilityPostcode(postcode);
    }
}
