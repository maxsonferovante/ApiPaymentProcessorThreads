package com.maal.apipaymentprocessorthreads.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.adapter.http.PaymentProcessorClient;
import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;


@Configuration
public class PaymentHttpClientConfiguration {

    @Bean(name = "paymentProcessorDefaultHttpClient")
    public PaymentProcessorManualClient defaultClient(@Value("${app.payment-processor.default.url}") String url,
                                                      HttpClient httpClient,
                                                      ObjectMapper objectMapper) {

        return new PaymentProcessorClient(url, httpClient, objectMapper);
    }
    @Bean("paymentProcessorFallbackHttpClient")
    public PaymentProcessorManualClient fallbackClient(@Value("${app.payment-processor.fallback.url}") String url,
                                                       HttpClient httpClient,
                                                       ObjectMapper objectMapper) {

        return new PaymentProcessorClient(url, httpClient, objectMapper);
    }
}
