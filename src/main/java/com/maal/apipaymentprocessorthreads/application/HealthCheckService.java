package com.maal.apipaymentprocessorthreads.application;


import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor(); // Para executar health checks em Virtual Threads
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final AtomicBoolean isDefaultClientActive;
    private final AtomicBoolean isFallbackClientActive;

    // New fields to store full HealthStatus
    private final AtomicReference<HealthStatus> defaultHealthStatusRef = new AtomicReference<>(new HealthStatus(true, 0));
    private final AtomicReference<HealthStatus> fallbackHealthStatusRef = new AtomicReference<>(new HealthStatus(true, 0));

    public HealthCheckService( @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                               @Qualifier(value = "paymentProcessorFallbackHttpClient")PaymentProcessorManualClient paymentProcessorFallbackClient) {
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        isDefaultClientActive = new AtomicBoolean(true);
        isFallbackClientActive = new AtomicBoolean(false);
    }

    @PostConstruct
    private void init() {
        scheduler.scheduleAtFixedRate(this::healthCheck, 0, 4998, TimeUnit.MILLISECONDS);
    }

    @ConditionalOnProperty(
            name = "app.payment-processor.healthcheck.leader.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public void healthCheck(){
        try{
            CompletableFuture<HealthStatus> defaultCheck = CompletableFuture.supplyAsync(paymentProcessorDefaultClient::healthCheck, asyncExecutor);
            CompletableFuture<HealthStatus> fallbackCheck = CompletableFuture.supplyAsync(paymentProcessorFallbackClient::healthCheck, asyncExecutor);

            CompletableFuture.allOf(defaultCheck,fallbackCheck).get(5, TimeUnit.SECONDS);

            HealthStatus newDefaultStatus = defaultCheck.join();
            HealthStatus newFallbackStatus = fallbackCheck.join();

            isDefaultClientActive.set(!newDefaultStatus.failing());
            isFallbackClientActive.set(!newFallbackStatus.failing());

            // Update the AtomicReferences
            defaultHealthStatusRef.set(newDefaultStatus);
            fallbackHealthStatusRef.set(newFallbackStatus);

            logger.info("Result of default client health check {} - {}", newDefaultStatus, isDefaultClientActive.get());
            logger.info("Result of fallback client health check {} - {}", newFallbackStatus, isFallbackClientActive.get());

            logger.info("Health check completed successfully for both clients.");
        } catch (Exception e) {
            logger.error("Unexpected error during health check: {}", e.getMessage());
            // Se houve um erro na verificação, consideramos ambos os clientes como inativos para evitar problemas
            isDefaultClientActive.set(false);
            isFallbackClientActive.set(false);
            // Also update the AtomicReferences to indicate failure
            defaultHealthStatusRef.set(new HealthStatus(true, 0));
            fallbackHealthStatusRef.set(new HealthStatus(true, 0));

        }
    }

    public Boolean getDefaultClientActive() {
        return isDefaultClientActive.get();
    }

    public Boolean getFallbackClientActive() {
        return isFallbackClientActive.get();
    }

    // New methods to get the full HealthStatus objects
    public HealthStatus getDefaultHealthStatus() {
        return defaultHealthStatusRef.get();
    }

    public HealthStatus getFallbackHealthStatus() {
        return fallbackHealthStatusRef.get();
    }
}
