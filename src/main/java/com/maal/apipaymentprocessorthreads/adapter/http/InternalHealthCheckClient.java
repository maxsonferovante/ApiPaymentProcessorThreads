package com.maal.apipaymentprocessorthreads.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;

public class InternalHealthCheckClient {
    
    private static final Logger logger = LoggerFactory.getLogger(InternalHealthCheckClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.of(3, SECONDS);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String leaderHealthCheckUrl;

    public InternalHealthCheckClient(HttpClient httpClient,
                                     ObjectMapper objectMapper,
                                     String leaderHealthCheckUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.leaderHealthCheckUrl = leaderHealthCheckUrl;
    }

    public Optional<HealthStatus> check() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(leaderHealthCheckUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                try {
                    HealthStatus healthStatus = objectMapper.readValue(response.body(), HealthStatus.class);
                    return Optional.of(healthStatus);
                } catch (Exception e) {
                    logger.warn("Failed to parse leader health check response as HealthStatus: {}", e.getMessage());
                    
                    try {
                        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                        boolean failing = (boolean) responseMap.getOrDefault("failing", false);
                        int minResponseTime = (int) responseMap.getOrDefault("minResponseTime", 0);
                        HealthStatus healthStatus = new HealthStatus(failing, minResponseTime);
                        return Optional.of(healthStatus);
                    } catch (Exception mapException) {
                        logger.warn("Failed to parse leader health check response as Map: {}", mapException.getMessage());
                        return Optional.empty();
                    }
                }
            } else {
                logger.warn("Leader health check failed with status code: {}", response.statusCode());
                return Optional.empty();
            }
        } catch (IOException e) {
            logger.debug("Leader health check IO error: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            logger.debug("Leader health check interrupted");
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Unexpected error during leader health check: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
