package com.maal.apipaymentprocessorthreads.entrypoint.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentSummaryGetResponse {
    @JsonProperty("default")
    private final SummaryDetailsResponse defaultApi;
    
    @JsonProperty("fallback")
    private final SummaryDetailsResponse fallbackApi;

    @JsonCreator
    public PaymentSummaryGetResponse(
            @JsonProperty("default") SummaryDetailsResponse defaultApi,
            @JsonProperty("fallback") SummaryDetailsResponse fallbackApi) {
        this.defaultApi = defaultApi;
        this.fallbackApi = fallbackApi;
    }

    public SummaryDetailsResponse getDefaultApi() {
        return defaultApi;
    }

    public SummaryDetailsResponse getFallbackApi() {
        return fallbackApi;
    }

    @Override
    public String toString() {
        return "PaymentSummaryGetResponse{" +
                "defaultApi=" + defaultApi +
                ", fallbackApi=" + fallbackApi +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PaymentSummaryGetResponse that = (PaymentSummaryGetResponse) obj;
        return java.util.Objects.equals(defaultApi, that.defaultApi) &&
               java.util.Objects.equals(fallbackApi, that.fallbackApi);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defaultApi, fallbackApi);
    }
}
