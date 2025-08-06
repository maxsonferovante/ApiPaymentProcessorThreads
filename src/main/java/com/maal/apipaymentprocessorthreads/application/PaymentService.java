package com.maal.apipaymentprocessorthreads.application;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPriorityBlockingQueue;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentPriorityBlockingQueue paymentsQueue;
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final int maxRetries;
    private final int maxVirtualThreads;
    
    public PaymentService(PaymentPriorityBlockingQueue paymentsQueue,
                          @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                          @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                          ObjectMapper objectMapper,
                          MongoTemplate mongoTemplate,
                          @Value("${app.payment-processor.maxVirtualThreads}") int maxVirtualThreads,
                          @Value("${app.payment-processor.max-retries}") int maxRetries
    ) {
        this.paymentsQueue = paymentsQueue;
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
        this.maxVirtualThreads = maxVirtualThreads;
        this.maxRetries = maxRetries;
      
    }

    @PostConstruct
    public void init() {
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
                try{
                    processPayment(paymentProcess.get());
                }
                catch (Exception e) {
                    logger.error("Error processing payment: {}", e.getMessage(), e);
                }
            }
        }
    }

    private void processPayment(PaymentsProcess paymentsProcess) {
        boolean isProcessed = false;
        for (int i = 0; i < 15; i++) {
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
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!isProcessed) {
            if (paymentsProcess.retryCount() < maxRetries) {
                paymentsProcess.incrementRetryCount();
                paymentsQueue.addToLastQueue(paymentsProcess);
            }
            else {
                logger.warn("Payment with correlation ID {} failed after {} retries",
                            paymentsProcess.payment().correlationId(), maxRetries);
            }
        }
    }

    private void savePayment (PaymentsProcess paymentsProcess, PaymentProcessorType type){
        PaymentDocument paymentDocument = new PaymentDocument();
        paymentDocument.setCorrelationId(String.valueOf(paymentsProcess.payment().correlationId()));
        paymentDocument.setAmount(paymentsProcess.payment().amount());
        paymentDocument.setRequestedAt(paymentsProcess.payment().requestedAt());
        paymentDocument.setProcessorType(type);
        mongoTemplate.insert(paymentDocument, "payments");
    }

}