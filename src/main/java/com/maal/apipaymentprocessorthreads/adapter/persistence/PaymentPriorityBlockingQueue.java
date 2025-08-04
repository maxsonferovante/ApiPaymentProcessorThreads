package com.maal.apipaymentprocessorthreads.adapter.persistence;

import com.maal.apipaymentprocessorthreads.domain.model.PaymentsProcess;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class PaymentPriorityBlockingQueue {

    private final PriorityBlockingQueue<PaymentsProcess> paymentsQueue = new PriorityBlockingQueue<>();

    public Optional<PaymentsProcess> fetchPayment(){
        try {
            return Optional.of(paymentsQueue.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToQueue(PaymentsProcess paymentsProcess) {
        paymentsQueue.add(paymentsProcess);
    }

    public void addToLastQueue(PaymentsProcess paymentsProcess) {
        paymentsQueue.offer(paymentsProcess);
    }
}