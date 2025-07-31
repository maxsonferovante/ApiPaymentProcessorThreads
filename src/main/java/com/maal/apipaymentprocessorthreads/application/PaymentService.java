package com.maal.apipaymentprocessorthreads.application;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentLinkedBlockingQueue;
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

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentPersistenceMongo paymentPersistence;
    private final PaymentLinkedBlockingQueue paymentsQueue;
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final HealthCheckService healthCheckService;
    private final ObjectMapper objectMapper;


    @Value("${app.payment-processor.maxVirtualThreads}")
    private static int maxVirtualThreads;

    public PaymentService(PaymentPersistenceMongo paymentPersistence,
                          PaymentLinkedBlockingQueue paymentsQueue,
                          @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                          @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                          HealthCheckService healthCheckService,
                          ObjectMapper objectMapper) {
        this.paymentPersistence = paymentPersistence;
        this.paymentsQueue = paymentsQueue;
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.healthCheckService = healthCheckService;
        this.objectMapper = objectMapper;

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
        paymentsQueue.addToQueue(new PaymentsProcess(paymentAsJson, payment));
    }

    private void runWorker() {
        while (true) {
            try {
                Optional<PaymentsProcess> paymentProcess = paymentsQueue.fetchPayment();
                if (paymentProcess.isPresent()) {
                    processPayment(paymentProcess.get());
                }
            }
            catch (Exception e) {
                logger.error("Error processing payment: {}", e.getMessage());
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processPayment(PaymentsProcess paymentsProcess) {

        if (Boolean.TRUE.equals(healthCheckService.getDefaultHealthStatus())) {
            logger.info("Processing payment {}", paymentsProcess.Payment().getCorrelationId());
            boolean processed = paymentProcessorDefaultClient.processPayment(paymentsProcess.paymentInJson());
            if (Boolean.TRUE.equals(processed)) {
                logger.info("Payment processed successfully for correlation ID: {}", paymentsProcess.Payment().getCorrelationId());
                savePayment(paymentsProcess, PaymentProcessorType.DEFAULT);
            }
        } else {
            if (Boolean.TRUE.equals(healthCheckService.getFallbackHealthStatus())){
                logger.info("Processing payment {} with fallback client", paymentsProcess.Payment().getCorrelationId());
                boolean processed = paymentProcessorFallbackClient.processPayment(paymentsProcess.paymentInJson());
                if (processed) {
                    logger.info("Payment processed successfully with fallback client for correlation ID: {}", paymentsProcess.Payment().getCorrelationId());
                    savePayment(paymentsProcess, PaymentProcessorType.FALLBACK);
                }
            } else {
                logger.warn("Payment processing failed for correlation ID: {} with fallback client", paymentsProcess.Payment().getCorrelationId());
                paymentsQueue.addToQueue(paymentsProcess);
            }
        }
    }

    private void savePayment (PaymentsProcess paymentsProcess, PaymentProcessorType type){
        PaymentDocument paymentDocument = new PaymentDocument();
        paymentDocument.setCorrelationId(String.valueOf(paymentsProcess.Payment().getCorrelationId()));
        paymentDocument.setAmount(paymentsProcess.Payment().getAmount());
        paymentDocument.setRequestedAt(paymentsProcess.Payment().getRequestedAt());
        paymentDocument.setProcessorType(type);
        paymentPersistence.save(paymentDocument);
    }
}