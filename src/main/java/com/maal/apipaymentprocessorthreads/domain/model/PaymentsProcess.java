package com.maal.apipaymentprocessorthreads.domain.model;

import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentRequest;

public record PaymentsProcess(String paymentInJson, Payment Payment){ }
