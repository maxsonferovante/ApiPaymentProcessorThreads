package com.maal.apipaymentprocessorthreads.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class InternalHealthCheckClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String leaderHealthCheckUrl;

    public InternalHealthCheckClient(HttpClient httpClient,
                                     ObjectMapper objectMapper,
                                     @Value("${app.payment-processor.healthcheck.leader.url}") String leaderHealthCheckUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.leaderHealthCheckUrl = leaderHealthCheckUrl;
    }

    public HealthStatus getDefaultHealthStatus() {
        return getHealthStatus(leaderHealthCheckUrl + "/default");
    }

    public HealthStatus getFallbackHealthStatus() {
        return getHealthStatus(leaderHealthCheckUrl + "/fallback");
    }

    private HealthStatus getHealthStatus(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), HealthStatus.class);
            } else {
                // Handle non-200 responses, perhaps log and return a failing status
                return new HealthStatus(true, 0);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new HealthStatus(true, 0);
        }
    }
}

