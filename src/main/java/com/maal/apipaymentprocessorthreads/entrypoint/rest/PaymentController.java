package com.maal.apipaymentprocessorthreads.entrypoint.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.maal.apipaymentprocessorthreads.application.PaymentService;
import com.maal.apipaymentprocessorthreads.application.PaymentSummaryService;
import com.maal.apipaymentprocessorthreads.application.PurgePaymentService;
import com.maal.apipaymentprocessorthreads.domain.model.Payment;
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

    public PaymentController(PaymentService paymentService,
                             PaymentSummaryService paymentSummaryService,
                             PurgePaymentService purgePaymentService) {
        this.paymentService = paymentService;
        this.paymentSummaryService = paymentSummaryService;
        this.purgePaymentService = purgePaymentService;
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
        return ResponseEntity.ok(healthStatus);
    }
}