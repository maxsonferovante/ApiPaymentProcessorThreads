package com.maal.apipaymentprocessorthreads.entrypoint.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentSummaryGetResponse(
        @JsonProperty(value = "default")
        SummaryDetailsResponse defaultApi,
        @JsonProperty(value = "fallback")
        SummaryDetailsResponse fallbackApi) { }
