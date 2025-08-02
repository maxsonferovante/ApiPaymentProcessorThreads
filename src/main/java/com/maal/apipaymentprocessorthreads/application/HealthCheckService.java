package com.maal.apipaymentprocessorthreads.application;


import com.maal.apipaymentprocessorthreads.adapter.http.InternalHealthCheckClient;
import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final AtomicBoolean isDefaultClientActive;
    private final AtomicBoolean isFallbackClientActive;

    // Health status references
    private final AtomicReference<HealthStatus> defaultHealthStatusRef = new AtomicReference<>(new HealthStatus(true, 0));
    private final AtomicReference<HealthStatus> fallbackHealthStatusRef = new AtomicReference<>(new HealthStatus(true, 0));

    // Leader-related fields
    private final Optional<InternalHealthCheckClient> internalHealthCheckClient;
    private final AtomicBoolean isLeaderActive = new AtomicBoolean(false);
    private final AtomicReference<HealthStatus> leaderHealthStatusRef = new AtomicReference<>(new HealthStatus(true, 0));
    private final boolean isLeaderInstance;
    
    // Failover mechanism
    private final AtomicBoolean shouldActAsLeader = new AtomicBoolean(false);
    private long lastLeaderCheckTime = 0;
    private final long leaderFailoverTimeout;
    private final long healthCheckInterval;
    
    // Rate limiting for health checks
    private long lastDefaultHealthCheck = 0;
    private long lastFallbackHealthCheck = 0;
    private static final long HEALTH_CHECK_RATE_LIMIT = 6000; // 6 seconds to be safe

    public HealthCheckService(@Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                              @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                              Optional<InternalHealthCheckClient> internalHealthCheckClient,
                              @Value("${app.payment-processor.healthcheck.leader.enabled:false}") boolean isLeaderInstance,
                              @Value("${app.payment-processor.healthcheck.failover.timeout:10000}") long leaderFailoverTimeout,
                              @Value("${app.payment-processor.healthcheck.interval:10000}") long healthCheckInterval) {
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.internalHealthCheckClient = internalHealthCheckClient;
        this.isLeaderInstance = isLeaderInstance;
        this.leaderFailoverTimeout = leaderFailoverTimeout;
        this.healthCheckInterval = healthCheckInterval;
        isDefaultClientActive = new AtomicBoolean(true);
        isFallbackClientActive = new AtomicBoolean(false);
    }

    @PostConstruct
    private void init() {
        // Start health check immediately and then every configured interval
        scheduler.scheduleAtFixedRate(this::healthCheck, 0, healthCheckInterval, TimeUnit.MILLISECONDS);
        
        // If this is a leader instance, also start leader health check
        if (isLeaderInstance) {
            shouldActAsLeader.set(true);
        }
    }

    public void healthCheck() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Rate limiting for Payment Processor health checks
            CompletableFuture<HealthStatus> defaultCheck = CompletableFuture.supplyAsync(() -> {
                if (currentTime - lastDefaultHealthCheck > HEALTH_CHECK_RATE_LIMIT) {
                    lastDefaultHealthCheck = currentTime;
                    return paymentProcessorDefaultClient.healthCheck();
                } else {
                    return defaultHealthStatusRef.get();
                }
            }, asyncExecutor);
            
            CompletableFuture<HealthStatus> fallbackCheck = CompletableFuture.supplyAsync(() -> {
                if (currentTime - lastFallbackHealthCheck > HEALTH_CHECK_RATE_LIMIT) {
                    lastFallbackHealthCheck = currentTime;
                    return paymentProcessorFallbackClient.healthCheck();
                } else {
                    return fallbackHealthStatusRef.get();
                }
            }, asyncExecutor);

            // Check leader health if configured
            CompletableFuture<Optional<HealthStatus>> leaderCheck = internalHealthCheckClient.map(client ->
                    CompletableFuture.supplyAsync(client::check, asyncExecutor)
            ).orElse(CompletableFuture.completedFuture(Optional.empty()));

            // Wait for all health checks with timeout
            CompletableFuture.allOf(defaultCheck, fallbackCheck, leaderCheck).get(5, TimeUnit.SECONDS);

            HealthStatus newDefaultStatus = defaultCheck.join();
            HealthStatus newFallbackStatus = fallbackCheck.join();
            Optional<HealthStatus> newLeaderStatusOpt = leaderCheck.join();

            // Update Payment Processor status
            boolean defaultActive = !newDefaultStatus.failing();
            boolean fallbackActive = !newFallbackStatus.failing();
            
            isDefaultClientActive.set(defaultActive);
            isFallbackClientActive.set(fallbackActive);
            defaultHealthStatusRef.set(newDefaultStatus);
            fallbackHealthStatusRef.set(newFallbackStatus);

            // Handle leader status and failover logic
            handleLeaderStatus(newLeaderStatusOpt);

        } catch (Exception e) {
            logger.error("Unexpected error during health check: {}", e.getMessage());
            handleHealthCheckFailure();
        }
    }

    private void handleLeaderStatus(Optional<HealthStatus> newLeaderStatusOpt) {
        long currentTime = System.currentTimeMillis();
        
        if (newLeaderStatusOpt.isPresent()) {
            HealthStatus leaderStatus = newLeaderStatusOpt.get();
            isLeaderActive.set(!leaderStatus.failing());
            leaderHealthStatusRef.set(leaderStatus);
            lastLeaderCheckTime = currentTime;
            
            // If leader is healthy, non-leader instances should not act as leader
            if (!leaderStatus.failing() && !isLeaderInstance) {
                shouldActAsLeader.set(false);
            }
        } else {
            // No leader health check available or failed
            if (!isLeaderInstance) {
                // Check if we should failover to leader role
                if (currentTime - lastLeaderCheckTime > leaderFailoverTimeout) {
                    shouldActAsLeader.set(true);
                    logger.warn("Leader health check failed for {}ms, activating failover mode", 
                        currentTime - lastLeaderCheckTime);
                }
            }
        }
    }

    private void handleHealthCheckFailure() {
        // In case of health check failure, be conservative
        isDefaultClientActive.set(false);
        isFallbackClientActive.set(false);
        isLeaderActive.set(false);
        
        // Reset health status to safe defaults
        defaultHealthStatusRef.set(new HealthStatus(true, 0));
        fallbackHealthStatusRef.set(new HealthStatus(true, 0));
        leaderHealthStatusRef.set(new HealthStatus(true, 0));
        
        // If this is a non-leader instance and we can't reach the leader, consider failover
        if (!isLeaderInstance) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLeaderCheckTime > leaderFailoverTimeout) {
                shouldActAsLeader.set(true);
                logger.warn("Health check failure and leader unreachable, activating failover mode");
            }
        }
    }

    public Boolean getDefaultClientActive() {
        return isDefaultClientActive.get();
    }

    public Boolean getFallbackClientActive() {
        return isFallbackClientActive.get();
    }

    public HealthStatus getDefaultHealthStatus() {
        return defaultHealthStatusRef.get();
    }

    public HealthStatus getFallbackHealthStatus() {
        return fallbackHealthStatusRef.get();
    }

    public Boolean getLeaderActive() {
        return isLeaderActive.get();
    }

    public HealthStatus getLeaderHealthStatus() {
        return leaderHealthStatusRef.get();
    }

    /**
     * Returns true if this instance should act as leader (either configured as leader or failover activated)
     */
    public Boolean shouldActAsLeader() {
        return shouldActAsLeader.get();
    }

    /**
     * Returns true if this instance was originally configured as leader
     */
    public Boolean isLeaderInstance() {
        return isLeaderInstance;
    }
}