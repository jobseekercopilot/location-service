package com.jobseekercopilot.locationservice.controller;

import com.jobseekercopilot.locationservice.model.CommuteContracts;
import com.jobseekercopilot.locationservice.service.CommuteOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/commutes")
public class InternalCommuteController {
    private final CommuteOrchestrationService commutes;

    public InternalCommuteController(CommuteOrchestrationService commutes) {
        this.commutes = commutes;
    }

    @PostMapping("/matrix")
    public CommuteContracts.CommuteMatrixResponse matrix(
            @Valid @RequestBody CommuteContracts.CommuteMatrixRequest request) {
        return commutes.matrix(request);
    }
}
