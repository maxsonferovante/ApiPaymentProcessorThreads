package com.maal.apipaymentprocessorthreads.config;


import com.mongodb.MongoClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoSettings {

    @Bean
    public MongoClientSettings mongoClient() {
        return MongoClientSettings.builder()
                .applyToConnectionPoolSettings(
                        builder ->
                                builder
                                        .maxSize(50)
                                        .minSize(10)

                )
                .build();
    }


}
