package com.maal.apipaymentprocessorthreads.entrypoint.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class SummaryDetailsResponse {
    private final int totalRequests;
    private final BigDecimal totalAmount;

    @JsonCreator
    public SummaryDetailsResponse(
            @JsonProperty("totalRequests") int totalRequests,
            @JsonProperty("totalAmount") BigDecimal totalAmount) {
        this.totalRequests = totalRequests;
        this.totalAmount = totalAmount;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    @Override
    public String toString() {
        return "SummaryDetailsResponse{" +
                "totalRequests=" + totalRequests +
                ", totalAmount=" + totalAmount +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SummaryDetailsResponse that = (SummaryDetailsResponse) obj;
        return totalRequests == that.totalRequests &&
               java.util.Objects.equals(totalAmount, that.totalAmount);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(totalRequests, totalAmount);
    }
}
