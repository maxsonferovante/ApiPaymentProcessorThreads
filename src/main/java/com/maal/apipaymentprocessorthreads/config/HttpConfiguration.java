package com.maal.apipaymentprocessorthreads.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .executor(Runnable::run)
                .connectTimeout(Duration.ofMillis(10000)) // Increased from 5000 to 10000ms
                .build();
    }
}
