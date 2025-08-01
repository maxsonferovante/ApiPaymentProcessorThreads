package com.maal.apipaymentprocessorthreads.application;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maal.apipaymentprocessorthreads.adapter.http.InternalHealthCheckClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentPersistenceMongo paymentPersistence;
    private final PaymentPriorityBlockingQueue paymentsQueue;
    private final PaymentProcessorManualClient paymentProcessorDefaultClient;
    private final PaymentProcessorManualClient paymentProcessorFallbackClient;
    private final HealthCheckService healthCheckService;
    private final Optional<InternalHealthCheckClient> internalHealthCheckClient;
    private final ObjectMapper objectMapper;
    private final boolean isLeader;

    public PaymentService(PaymentPersistenceMongo paymentPersistence,
                          PaymentPriorityBlockingQueue paymentsQueue,
                          @Qualifier(value = "paymentProcessorDefaultHttpClient") PaymentProcessorManualClient paymentProcessorDefaultClient,
                          @Qualifier(value = "paymentProcessorFallbackHttpClient") PaymentProcessorManualClient paymentProcessorFallbackClient,
                          HealthCheckService healthCheckService,
                          @Autowired(required = false) Optional<InternalHealthCheckClient> internalHealthCheckClient,
                          ObjectMapper objectMapper,
                          @Value("${app.payment-processor.maxVirtualThreads}") int maxVirtualThreads,
                          @Value("${app.payment-processor.healthcheck.leader.enabled}") boolean isLeader) {
        this.paymentPersistence = paymentPersistence;
        this.paymentsQueue = paymentsQueue;
        this.paymentProcessorDefaultClient = paymentProcessorDefaultClient;
        this.paymentProcessorFallbackClient = paymentProcessorFallbackClient;
        this.healthCheckService = healthCheckService;
        this.internalHealthCheckClient = internalHealthCheckClient;
        this.objectMapper = objectMapper;
        this.isLeader = isLeader;
        logger.info("Payment service started. Max virtual threads: {}, Leader enabled: {}", maxVirtualThreads, isLeader);
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
            var paymentProcess = paymentsQueue.fetchPayment();
            if (paymentProcess.isPresent()) {
                processPayment(paymentProcess.get());
            }
        }
    }

    private void processPayment(PaymentsProcess paymentsProcess) {

        boolean defaultClientActive;
        boolean fallbackClientActive;
        boolean isProcessedDefault = false;
        boolean isProcessedFallback = false;

        if (isLeader) {
            defaultClientActive = healthCheckService.getDefaultClientActive();
            fallbackClientActive = healthCheckService.getFallbackClientActive();
        } else {
            InternalHealthCheckClient client = internalHealthCheckClient.orElseThrow(() -> new IllegalStateException("InternalHealthCheckClient not present for non-leader instance"));
            defaultClientActive = !client.getDefaultHealthStatus().failing();
            fallbackClientActive = !client.getFallbackHealthStatus().failing();
        }
        if (defaultClientActive) {
            logger.warn("Attempting to process payment {} with default client", paymentsProcess.payment().correlationId());
            isProcessedDefault  = paymentProcessorDefaultClient.processPayment(paymentsProcess.paymentInJson());
            if (isProcessedDefault) {
                logger.info("Payment processed successfully for correlation ID: {}", paymentsProcess.payment().correlationId());
                savePayment(paymentsProcess, PaymentProcessorType.DEFAULT);
            }
        }
        if (!isProcessedDefault && fallbackClientActive){
                logger.warn("Attempting to process payment {} with fallback client", paymentsProcess.payment().correlationId());
                isProcessedFallback = paymentProcessorFallbackClient.processPayment(paymentsProcess.paymentInJson());
                if (isProcessedFallback) {
                    logger.info("Payment processed successfully with fallback client for correlation ID: {}", paymentsProcess.payment().correlationId());
                    savePayment(paymentsProcess, PaymentProcessorType.FALLBACK);
                }
        }
        if (!isProcessedDefault && !isProcessedFallback) {
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
        logger.info("Payment saved successfully for correlation ID: {}", paymentsProcess.payment().correlationId());
    }
}