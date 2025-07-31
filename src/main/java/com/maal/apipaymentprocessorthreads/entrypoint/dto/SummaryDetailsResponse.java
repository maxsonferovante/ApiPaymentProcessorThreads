package com.maal.apipaymentprocessorthreads.entrypoint.dto;

import java.math.BigDecimal;

public record SummaryDetailsResponse(int totalRequests, BigDecimal totalAmount) {
}
