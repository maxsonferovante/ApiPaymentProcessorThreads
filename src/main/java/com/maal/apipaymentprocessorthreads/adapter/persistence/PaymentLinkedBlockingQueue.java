package com.maal.apipaymentprocessorthreads.adapter.persistence;

import com.maal.apipaymentprocessorthreads.domain.model.PaymentsProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class PaymentLinkedBlockingQueue {

    private final LinkedBlockingQueue<PaymentsProcess> paymentsQueue = new LinkedBlockingQueue<>();

    public Optional<PaymentsProcess> fetchPayment(){
        try {
            return Optional.of(paymentsQueue.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public void addToQueue(PaymentsProcess paymentsProcess) {
        try {
            paymentsQueue.put(paymentsProcess);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
