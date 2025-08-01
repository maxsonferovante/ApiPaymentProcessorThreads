package com.maal.apipaymentprocessorthreads.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(UUID correlationId, BigDecimal amount, Instant requestedAt) {
    public Payment(UUID correlationId, BigDecimal amount) {
        this(correlationId, amount, Instant.now());
    }
}
