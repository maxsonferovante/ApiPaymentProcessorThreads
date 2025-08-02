package com.maal.apipaymentprocessorthreads.entrypoint.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.maal.apipaymentprocessorthreads.application.HealthCheckService;
import com.maal.apipaymentprocessorthreads.application.PaymentService;
import com.maal.apipaymentprocessorthreads.application.PaymentSummaryService;
import com.maal.apipaymentprocessorthreads.application.PurgePaymentService;
import com.maal.apipaymentprocessorthreads.domain.model.Payment;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.HealthStatus;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentRequest;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentSummaryGetResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSummaryService paymentSummaryService;
    private final PurgePaymentService purgePaymentService;
    private final HealthCheckService healthCheckService;

    public PaymentController(PaymentService paymentService,
                             PaymentSummaryService paymentSummaryService,
                             PurgePaymentService purgePaymentService,
                             HealthCheckService healthCheckService) {
        this.paymentService = paymentService;
        this.paymentSummaryService = paymentSummaryService;
        this.purgePaymentService = purgePaymentService;
        this.healthCheckService = healthCheckService;
    }

    @PostMapping(value = "/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receivePayment(@RequestBody PaymentRequest request) throws JsonProcessingException {
        paymentService.paymentRequest(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/payments-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentSummaryGetResponse> getPaymentSummary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ResponseEntity.ok(
                paymentSummaryService.summary(from, to)
        );
    }

    @PostMapping("/purge-payments")
    public ResponseEntity<Void> purgePayments() {
        purgePaymentService.purgePayments();
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        healthStatus.put("default-client", healthCheckService.getDefaultHealthStatus());
        healthStatus.put("fallback-client", healthCheckService.getFallbackHealthStatus());
        healthStatus.put("leader", healthCheckService.getLeaderHealthStatus());
        
        healthStatus.put("is-leader-instance", healthCheckService.isLeaderInstance());
        healthStatus.put("should-act-as-leader", healthCheckService.shouldActAsLeader());
        healthStatus.put("default-client-active", healthCheckService.getDefaultClientActive());
        healthStatus.put("fallback-client-active", healthCheckService.getFallbackClientActive());
        healthStatus.put("leader-active", healthCheckService.getLeaderActive());
        
        return ResponseEntity.ok(healthStatus);
    }
}