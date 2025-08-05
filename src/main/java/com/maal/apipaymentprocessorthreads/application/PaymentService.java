package com.maal.apipaymentprocessorthreads.application;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPriorityBlockingQueue;
import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPersistenceMongo;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentDocument;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentProcessorType;
import com.maal.apipaymentprocessorthreads.domain.interfaces.PaymentProcessorManualClient;
import com.maal.apipaymentprocessorthreads.domain.model.Payment;
import com.maal.apipaymentprocessorthreads.domain.model.PaymentsProcess;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentPersistenceMongo paymentPersistence;
    private final PaymentPriorityBlockingQueue paymentsQueue;
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final int maxRetries;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    public PaymentService(PaymentPersistenceMongo paymentPersistence,
                          PaymentPriorityBlockingQueue paymentsQueue,
                          @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                          @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                          ObjectMapper objectMapper,
                          @Value("${app.payment-processor.maxVirtualThreads}") int maxVirtualThreads,
                          @Value("${app.payment-processor.max-retries}") int maxRetries
    ) {
        this.paymentPersistence = paymentPersistence;
        this.paymentsQueue = paymentsQueue;
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
        for (int i = 0; i < maxVirtualThreads; i++) {
            executor.submit(this::runWorker);
        }
        logger.info("Payment service started. Max virtual threads: {}", maxVirtualThreads);
    }

    public void paymentRequest(PaymentRequest request) throws JsonProcessingException {
        Payment payment = new Payment(
                request.correlationId(),
                request.amount()
        );
        String paymentAsJson = objectMapper.writeValueAsString(payment);
        paymentsQueue.addToQueue(new PaymentsProcess(paymentAsJson, payment, 0));
    }

    private void runWorker() {
        while (true) {
            var paymentProcess = paymentsQueue.fetchPayment();
            if (paymentProcess.isPresent()) {
                processPayment(paymentProcess.get());
            }
        }
    }

    private void processPayment(PaymentsProcess paymentsProcess) {
        boolean isProcessed = false;
        for (int i = 0; i < 15; i++) {
            logger.info("Attempt {} to process payment for correlation ID: {}", i + 1, paymentsProcess.payment().correlationId());
            if (paymentProcessorDefaultClient.processPayment(paymentsProcess.paymentInJson())) {
                savePayment(paymentsProcess, PaymentProcessorType.DEFAULT);
                isProcessed = true;
                break;
            }

            if (paymentProcessorFallbackClient.processPayment(paymentsProcess.paymentInJson())) {
                savePayment(paymentsProcess, PaymentProcessorType.FALLBACK);
                isProcessed = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.warn("Thread interrupted while waiting to retry payment for correlation ID: {}",
                        paymentsProcess.payment().correlationId());
                Thread.currentThread().interrupt();
            }
        }

        if (!isProcessed) {
            logger.warn("Payment processing failed for correlation ID: {}. Re-queuing with {} retries...",
                    paymentsProcess.payment().correlationId(), paymentsProcess.retryCount() + 1);
            if (paymentsProcess.retryCount() < maxRetries) {
                paymentsProcess.incrementRetryCount();
                paymentsQueue.addToLastQueue(paymentsProcess);
            } else {
                logger.warn("Payment processing failed for correlation ID: {}. Max retries reached.",
                        paymentsProcess.payment().correlationId());
            }
        }
    }

    private void savePayment (PaymentsProcess paymentsProcess, PaymentProcessorType type){
        PaymentDocument paymentDocument = new PaymentDocument();
        paymentDocument.setCorrelationId(String.valueOf(paymentsProcess.payment().correlationId()));
        paymentDocument.setAmount(paymentsProcess.payment().amount());
        paymentDocument.setRequestedAt(paymentsProcess.payment().requestedAt());
        paymentDocument.setProcessorType(type);
        paymentPersistence.save(paymentDocument);
        logger.info("Payment saved successfully for correlation ID {} with type {}", paymentsProcess.payment().correlationId(), type);

    }

    public boolean testPaymentProcessor(String processorType) throws JsonProcessingException {
        Payment testPayment = new Payment(
                java.util.UUID.randomUUID(),
                new java.math.BigDecimal("10.00")
        );
        String testPaymentJson = objectMapper.writeValueAsString(testPayment);
        
        boolean success = false;
        if ("default".equals(processorType)) {
            success = paymentProcessorDefaultClient.processPayment(testPaymentJson);
        } else if ("fallback".equals(processorType)) {
            success = paymentProcessorFallbackClient.processPayment(testPaymentJson);
        }
        
        return success;
    }
}