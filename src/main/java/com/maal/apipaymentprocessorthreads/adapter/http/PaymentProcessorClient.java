package com.maal.apipaymentprocessorthreads.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.time.Duration.ofMillis;

public class PaymentProcessorClient implements PaymentProcessorManualClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorClient.class);
    private static final Duration TIMEOUT = ofMillis(5000);
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PaymentProcessorClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean processPayment(String requestBody) {
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .timeout(TIMEOUT)
                    .uri(URI.create(baseUrl + "/payments"))
                    .POST(ofString(requestBody))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        catch (Exception e) {
            logger.warn("Payment processing failed for Payment Processor Client: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public HealthStatus healthCheck() {
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .timeout(TIMEOUT)
                    .uri(java.net.URI.create(baseUrl + "/payments/service-health"))
                    .GET()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            logger.info("Health check response: {}", responseMap);
            return new HealthStatus(
                    (boolean) responseMap.getOrDefault("failing", false),
                    (int) responseMap.getOrDefault("minResponseTime", 0)
            );

        }
        catch (Exception e) {
            logger.warn("Health check failed for Payment Processor Client: {}", e.getMessage());
            return new HealthStatus(false, 0);
        }

    }
}
