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



@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentPersistenceMongo paymentPersistence;
    private final PaymentPriorityBlockingQueue paymentsQueue;
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final HealthCheckService healthCheckService;
    private final int maxRetries;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentPersistenceMongo paymentPersistence,
                          PaymentPriorityBlockingQueue paymentsQueue,
                          @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                          @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                          HealthCheckService healthCheckService,
                          ObjectMapper objectMapper,
                          @Value("${app.payment-processor.maxVirtualThreads}") int maxVirtualThreads,
                          @Value("${app.payment-processor.max-retries}") int maxRetries) {
        this.paymentPersistence = paymentPersistence;
        this.paymentsQueue = paymentsQueue;
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.healthCheckService = healthCheckService;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
        logger.info("Payment service started. Max virtual threads: {}", maxVirtualThreads);
        for (int i = 0; i < maxVirtualThreads; i++) {
            Thread.startVirtualThread(this::runWorker);
        }
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
        if (paymentsProcess.retryCount() > maxRetries) {
            logger.warn("Payment processing failed for correlation ID: {}. Max retries reached.",
                    paymentsProcess.payment().correlationId());
            return;
        }

        boolean defaultClientActive = healthCheckService.getDefaultClientActive();
        boolean fallbackClientActive = healthCheckService.getFallbackClientActive();

        boolean isProcessed = false;

        if (defaultClientActive) {
            isProcessed = paymentProcessorDefaultClient.processPayment(paymentsProcess.paymentInJson());
            if (isProcessed) {
                savePayment(paymentsProcess, PaymentProcessorType.DEFAULT);
            }
        }

        if (!isProcessed && fallbackClientActive) {
            isProcessed = paymentProcessorFallbackClient.processPayment(paymentsProcess.paymentInJson());
            if (isProcessed) {
                savePayment(paymentsProcess, PaymentProcessorType.FALLBACK);
            }
        }

        if (!isProcessed) {
            logger.warn("Payment processing failed for correlation ID: {}. Re-queuing with {} retries...",
                    paymentsProcess.payment().correlationId(), paymentsProcess.retryCount() + 1);
            paymentsQueue.addToQueue(new PaymentsProcess(
                    paymentsProcess.paymentInJson(),
                    paymentsProcess.payment(),
                    paymentsProcess.retryCount() + 1
            ));
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