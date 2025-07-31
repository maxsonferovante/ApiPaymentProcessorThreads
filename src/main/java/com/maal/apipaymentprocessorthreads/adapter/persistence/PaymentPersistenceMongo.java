package com.maal.apipaymentprocessorthreads.adapter.persistence;

import com.maal.apipaymentprocessorthreads.domain.document.PaymentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentPersistenceMongo extends MongoRepository<PaymentDocument, String> {
}
