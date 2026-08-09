package com.jobseekercopilot.locationservice.controller;

import com.jobseekercopilot.locationservice.model.CanonicalLocation;
import com.jobseekercopilot.locationservice.model.LocationContracts;
import com.jobseekercopilot.locationservice.service.LocationOrchestrationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalLocationController {
    private final LocationOrchestrationService locations;

    public InternalLocationController(LocationOrchestrationService locations) {
        this.locations = locations;
    }

    @PostMapping("/locations/autocomplete")
    public LocationContracts.AutocompleteResponse autocomplete(
            @Valid @RequestBody LocationContracts.AutocompleteRequest request) {
        return locations.autocomplete(request);
    }

    @PostMapping("/locations/resolve")
    public LocationContracts.ResolveResponse resolve(
            @Valid @RequestBody LocationContracts.ResolveRequest request) {
        return locations.resolve(request);
    }

    @GetMapping("/postcodes/{postcode}")
    public CanonicalLocation lookup(@PathVariable String postcode) {
        return locations.lookupPostcode(postcode);
    }

    @GetMapping("/legacy/locations")
    public List<LocationContracts.LegacyLocation> legacySearch(@RequestParam("q") String query) {
        return locations.legacySearch(query);
    }

    @GetMapping("/legacy/postcodes/{postcode}")
    public LocationContracts.LegacyLocation legacyLookup(@PathVariable String postcode) {
        return locations.legacyLookup(postcode);
    }
}
