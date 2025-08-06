package com.maal.apipaymentprocessorthreads.domain.interfaces;

public interface PaymentProcessorManualClient {
    boolean processPayment(String requestBody);
}
