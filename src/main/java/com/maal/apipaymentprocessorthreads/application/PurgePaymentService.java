package com.maal.apipaymentprocessorthreads.application;


import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPersistenceMongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PurgePaymentService {

    private final Logger logger = LoggerFactory.getLogger(PurgePaymentService.class);

    private final PaymentPersistenceMongo paymentPersistence;

    public PurgePaymentService(PaymentPersistenceMongo paymentPersistence) {
        this.paymentPersistence = paymentPersistence;
    }

    public void purgePayments() {
        logger.info("Purging all payments from the database");
        paymentPersistence.deleteAll();
        logger.info("All payments have been purged successfully");
    }
}
