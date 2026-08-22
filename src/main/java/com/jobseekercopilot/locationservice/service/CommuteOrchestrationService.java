package com.jobseekercopilot.locationservice.service;

import com.jobseekercopilot.locationservice.client.GoogleMapsGatewayClient;
import com.jobseekercopilot.locationservice.model.CommuteContracts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommuteOrchestrationService {
    private final GoogleMapsGatewayClient googleClient;
    private final boolean googleEnabled;
    private final int maximumDestinations;

    public CommuteOrchestrationService(
            GoogleMapsGatewayClient googleClient,
            @Value("${google.maps.enabled:false}") boolean googleEnabled,
            @Value("${location.commute.maximum-destinations:5}") int maximumDestinations) {
        this.googleClient = googleClient;
        this.googleEnabled = googleEnabled;
        this.maximumDestinations = maximumDestinations;
    }

    public CommuteContracts.CommuteMatrixResponse matrix(CommuteContracts.CommuteMatrixRequest request) {
        if (!googleEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Commute provider is disabled");
        }
        if (request.destinations().size() > maximumDestinations) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many commute destinations");
        }
        if (request.origin().precision() == com.jobseekercopilot.locationservice.model.LocationEnums.Precision.NONE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Origin is not routable");
        }
        return googleClient.matrix(request);
    }
}
