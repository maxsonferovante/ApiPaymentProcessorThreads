package com.maal.apipaymentprocessorthreads.adapter.persistence;

import com.maal.apipaymentprocessorthreads.domain.document.PaymentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PaymentPersistenceMongo extends MongoRepository<PaymentDocument, String> {
    
    @Query(value = "{'requestedAt': {$gte: ?0, $lte: ?1}}")
    List<PaymentDocument> findByRequestedAtBetween(Instant from, Instant to);
    
    @Query(value = "{'processorType': ?0, 'requestedAt': {$gte: ?1, $lte: ?2}}")
    List<PaymentDocument> findByProcessorTypeAndRequestedAtBetween(
        String processorType, Instant from, Instant to);
}
