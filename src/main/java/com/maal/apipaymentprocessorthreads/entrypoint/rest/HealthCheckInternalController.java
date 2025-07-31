package com.maal.apipaymentprocessorthreads.entrypoint.rest;

import com.maal.apipaymentprocessorthreads.application.HealthCheckService;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/health")
@ConditionalOnProperty(
        name = "app.payment-processor.healthcheck.leader.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class HealthCheckInternalController {

    private final HealthCheckService healthCheckService;

    public HealthCheckInternalController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/default")
    public HealthStatus getDefaultHealthStatus() {
        return healthCheckService.getDefaultHealthStatus();
    }

    @GetMapping("/fallback")
    public HealthStatus getFallbackHealthStatus() {
        return healthCheckService.getFallbackHealthStatus();
    }
}
