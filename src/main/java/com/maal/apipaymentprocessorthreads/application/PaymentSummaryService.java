package com.maal.apipaymentprocessorthreads.application;


import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPersistenceMongo;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentProcessorType;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentSummaryGetResponse;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.SummaryDetailsResponse;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSummaryService.class);

    private final MongoTemplate mongoTemplate;

    public PaymentSummaryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public PaymentSummaryGetResponse summary(Instant from, Instant to) {
        try {
            logger.info("Searching for payment summary from {} to {}", from, to);
            // caso não tenha from e to, pega tudo, sem intervalo de datas, logo agregation não terá filtro de data
            if (from == null && to == null) {
                from = Instant.EPOCH; // Data inicial padrão
                to = Instant.now(); // Data final padrão
            } else if (from == null) {
                from = Instant.EPOCH; // Data inicial padrão
            } else if (to == null) {
                to = Instant.now(); // Data final padrão
            }
            Criteria dateCriteria = Criteria.where("requestedAt")
                    .gte(from)
                    .lte(to);

            Aggregation aggregation = newAggregation(
                    match(dateCriteria),
                    group("processorType")
                            .sum("amount").as("totalAmount")
                            .count().as("totalRequests")
            );

            var results = mongoTemplate.aggregate(aggregation, "payments", Document.class).getMappedResults();

            BigDecimal defaultAmount = BigDecimal.ZERO;
            int defaultRequests = 0;
            BigDecimal fallbackAmount = BigDecimal.ZERO;
            int fallbackRequests = 0;

            for (Document doc : results) {
                String processorTypeString = doc.getString("_id");

                Number amountNumber = doc.get("totalAmount", Number.class);
                BigDecimal amount = amountNumber != null
                        ? new BigDecimal(amountNumber.toString())
                        : BigDecimal.ZERO;

                int count = doc.getInteger("totalRequests", 0);

                if (PaymentProcessorType.DEFAULT.name().equals(processorTypeString)) {
                    defaultAmount = amount;
                    defaultRequests = count;
                }  else if (PaymentProcessorType.FALLBACK.name().equals(processorTypeString)) { // Adicionado else if para Fallback
                    fallbackAmount = amount;
                    fallbackRequests = count;
                }
            }
            var response = new PaymentSummaryGetResponse(
                    new SummaryDetailsResponse(defaultRequests, defaultAmount),
                    new SummaryDetailsResponse(fallbackRequests, fallbackAmount)
            );
            logger.info("Payment summary retrieved successfully from {} to {} -> {}", from, to, response);
            return response;

        } catch (Exception e) {
            logger.error("Error retrieving payment summary: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve payment summary", e);
        }
    }

}
