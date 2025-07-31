package com.maal.apipaymentprocessorthreads.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("payments")
public class PaymentDocument {

    @Id
    private String id;

    private String correlationId;

    @Field(value = "amount", targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private Instant requestedAt;

    private PaymentProcessorType processorType;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public PaymentProcessorType getProcessorType() {
        return processorType;
    }

    public void setProcessorType(PaymentProcessorType processorType) {
        this.processorType = processorType;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
