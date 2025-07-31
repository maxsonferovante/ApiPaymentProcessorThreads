package com.maal.apipaymentprocessorthreads.application;


import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPersistenceMongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class PurgePaymentService {

    private final Logger logger = LoggerFactory.getLogger(PurgePaymentService.class);

    private final PaymentPersistenceMongo paymentPersistence;
    private final MongoTemplate mongoTemplate;

    public PurgePaymentService(PaymentPersistenceMongo paymentPersistence, MongoTemplate mongoTemplate) {
        this.paymentPersistence = paymentPersistence;
        this.mongoTemplate = mongoTemplate;
    }

    public void purgePayments() {
        logger.info("Purging all payments from the database");
        paymentPersistence.deleteAll();
        mongoTemplate.getCollection("payments").drop();
        logger.info("All payments have been purged successfully");
    }
}
