package com.maal.apipaymentprocessorthreads.entrypoint.dto;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID correlationId, BigDecimal amount) {
    public PaymentRequest {
        if (correlationId == null) {
            correlationId = UUID.randomUUID();
        }
    }
}
