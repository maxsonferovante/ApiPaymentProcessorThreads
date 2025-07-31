package com.maal.apipaymentprocessorthreads.adapter.http;

import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "app.payment-processor.healthcheck.leader.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InternalHealthCheckClient {

    private final RestTemplate restTemplate;
    private final String leaderHealthCheckUrl;

    public InternalHealthCheckClient(RestTemplate restTemplate,
                                     @Value("${app.payment-processor.healthcheck.leader.url}") String leaderHealthCheckUrl) {
        this.restTemplate = restTemplate;
        this.leaderHealthCheckUrl = leaderHealthCheckUrl;
    }

    public HealthStatus getDefaultHealthStatus() {
        return restTemplate.getForObject(leaderHealthCheckUrl + "/default", HealthStatus.class);
    }

    public HealthStatus getFallbackHealthStatus() {
        return restTemplate.getForObject(leaderHealthCheckUrl + "/fallback", HealthStatus.class);
    }
}
