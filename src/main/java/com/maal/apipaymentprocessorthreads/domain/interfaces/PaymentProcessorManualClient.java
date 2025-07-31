package com.maal.apipaymentprocessorthreads.domain.interfaces;

import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;

public interface PaymentProcessorManualClient {
    boolean processPayment(String requestBody);
    HealthStatus healthCheck();
}
