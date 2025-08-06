package com.maal.apipaymentprocessorthreads.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document("payments")
@CompoundIndexes({
    @CompoundIndex(name = "processor_requested", def = "{'processorType': 1, 'requestedAt': 1}"),
    @CompoundIndex(name = "requested_processor", def = "{'requestedAt': 1, 'processorType': 1}")
})
public class PaymentDocument {

    @Id
    private String id;

    @Indexed(unique = true, background = true)
    private String correlationId;

    @Field(value = "amount", targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @Indexed(background = true)
    private Instant requestedAt;

    @Indexed(background = true)
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
