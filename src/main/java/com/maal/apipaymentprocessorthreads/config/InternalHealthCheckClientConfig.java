package com.maal.apipaymentprocessorthreads.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.adapter.http.InternalHealthCheckClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(
        name = "app.payment-processor.healthcheck.leader.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InternalHealthCheckClientConfig {

    @Bean
    public String leaderHealthCheckUrl(@Value("${app.payment-processor.healthcheck.leader.url}") String leaderHealthCheckUrl) {
        return leaderHealthCheckUrl;
    }

    @Bean
    public InternalHealthCheckClient internalHealthCheckClient(HttpClient httpClient,
                                                               ObjectMapper objectMapper,
                                                               String leaderHealthCheckUrl) {
        return new InternalHealthCheckClient(httpClient, objectMapper, leaderHealthCheckUrl);
    }
}
