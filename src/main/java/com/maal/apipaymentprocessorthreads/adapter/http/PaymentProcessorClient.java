package com.maal.apipaymentprocessorthreads.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.time.Duration.ofMillis;

public class PaymentProcessorClient implements PaymentProcessorManualClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorClient.class);
    private static final Duration TIMEOUT = ofMillis(10000);
    private final String baseUrl;
    private final HttpClient httpClient;


    public PaymentProcessorClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public boolean processPayment(String requestBody) {
        try {
            String url = baseUrl + "/payments";

            HttpRequest request = HttpRequest.newBuilder()
                    .timeout(TIMEOUT)
                    .uri(URI.create(url))
                    .POST(ofString(requestBody))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return isSuccessfulResponse(response.statusCode(), response.body());
        }
        catch (Exception e) {
            logger.warn("Processor Client error: {}", e.getMessage());
            return false;
        }
    }

    private boolean isSuccessfulResponse(int statusCode, String responseBody) {
        return isSuccessful200Response(statusCode, responseBody) || isDuplicatePayment422Response(statusCode, responseBody);
    }

    private boolean isSuccessful200Response(int statusCode, String responseBody) {
        return statusCode == 200 && responseBody.contains("payment processed successfully");
    }

    private boolean isDuplicatePayment422Response(int statusCode, String responseBody) {
        return statusCode == 422 && responseBody.toLowerCase().contains("correlationid already exists");
    }
}
