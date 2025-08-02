package com.maal.apipaymentprocessorthreads.entrypoint.dto;

import com.maal.apipaymentprocessorthreads.domain.document.PaymentProcessorType;

import java.math.BigDecimal;
import java.util.Objects;

public class PaymentSummaryAggregationResult {
    private PaymentProcessorType processorType;
    private BigDecimal totalAmount;
    private int totalRequests;

    public PaymentSummaryAggregationResult() {
    }

    public PaymentSummaryAggregationResult(PaymentProcessorType processorType, BigDecimal totalAmount, int totalRequests) {
        this.processorType = processorType;
        this.totalAmount = totalAmount;
        this.totalRequests = totalRequests;
    }

    public PaymentProcessorType getProcessorType() {
        return processorType;
    }

    public void setProcessorType(PaymentProcessorType processorType) {
        this.processorType = processorType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentSummaryAggregationResult that = (PaymentSummaryAggregationResult) o;
        return totalRequests == that.totalRequests && processorType == that.processorType && Objects.equals(totalAmount, that.totalAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processorType, totalAmount, totalRequests);
    }

    @Override
    public String toString() {
        return "PaymentSummaryAggregationResult{" +
                "processorType=" + processorType +
                ", totalAmount=" + totalAmount +
                ", totalRequests=" + totalRequests +
                '}';
    }
}