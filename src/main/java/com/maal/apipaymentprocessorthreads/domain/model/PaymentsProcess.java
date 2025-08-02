package com.maal.apipaymentprocessorthreads.domain.model;

import java.util.Objects;

public class PaymentsProcess implements Comparable<PaymentsProcess> {

    private final String paymentInJson;
    private final Payment payment;
    private int retryCount;

    public PaymentsProcess(String paymentInJson, Payment payment) {
        this(paymentInJson, payment, 0);
    }

    public PaymentsProcess(String paymentInJson, Payment payment, int retryCount) {
        this.paymentInJson = paymentInJson;
        this.payment = payment;
        this.retryCount = retryCount;
    }

    public String paymentInJson() {
        return paymentInJson;
    }

    public Payment payment() {
        return payment;
    }

    public int retryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public int compareTo(PaymentsProcess other) {
        return Integer.compare(this.retryCount, other.retryCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentsProcess that = (PaymentsProcess) o;
        return retryCount == that.retryCount &&
                Objects.equals(paymentInJson, that.paymentInJson) &&
                Objects.equals(payment, that.payment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentInJson, payment, retryCount);
    }
}
