package com.maal.apipaymentprocessorthreads.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .executor(Runnable::run)
                .connectTimeout(java.time.Duration.ofMillis(5000))
                .build();
    }
}
