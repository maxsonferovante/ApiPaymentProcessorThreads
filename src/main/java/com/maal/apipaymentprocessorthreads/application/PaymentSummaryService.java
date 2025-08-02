package com.maal.apipaymentprocessorthreads.application;

import com.maal.apipaymentprocessorthreads.adapter.persistence.PaymentPersistenceMongo;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentProcessorType;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentSummaryGetResponse;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.SummaryDetailsResponse;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Service
public class PaymentSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSummaryService.class);

    private final MongoTemplate mongoTemplate;

    public PaymentSummaryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public PaymentSummaryGetResponse summary(Instant from, Instant to) {
        try {
            if (from == null && to == null) {
                from = Instant.EPOCH;
                to = Instant.now();
            } else if (from == null) {
                from = Instant.EPOCH;
            } else if (to == null) {
                to = Instant.now();
            }
            
            return summaryWithAggregation(from, to);

        } catch (Exception e) {
            logger.error("Error retrieving payment summary: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve payment summary", e);
        }
    }

    private PaymentSummaryGetResponse summaryWithAggregation(Instant from, Instant to) {
        Criteria dateCriteria = Criteria.where("requestedAt")
                .gte(from)
                .lte(to);

        MatchOperation matchOperation = match(dateCriteria);
        GroupOperation groupOperation = group("processorType")
                .sum("amount").as("totalAmount")
                .count().as("totalRequests");
        ProjectionOperation projectionOperation = project()
                .andExpression("_id").as("processorType")
                .andExpression("totalAmount").as("totalAmount")
                .andExpression("totalRequests").as("totalRequests");

        Aggregation aggregation = newAggregation(matchOperation, groupOperation, projectionOperation);
        
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "payments", Map.class);
        
        BigDecimal defaultAmount = BigDecimal.ZERO;
        int defaultRequests = 0;
        BigDecimal fallbackAmount = BigDecimal.ZERO;
        int fallbackRequests = 0;

        for (Map result : results.getMappedResults()) {
            try {
                String processorType = (String) result.get("processorType");
                BigDecimal totalAmount = new BigDecimal(result.get("totalAmount").toString());
                int totalRequests = (Integer) result.get("totalRequests");

                if ("DEFAULT".equals(processorType)) {
                    defaultAmount = totalAmount;
                    defaultRequests = totalRequests;
                } else if ("FALLBACK".equals(processorType)) {
                    fallbackAmount = totalAmount;
                    fallbackRequests = totalRequests;
                }
            } catch (Exception e) {
                logger.warn("Error processing aggregation result: {}", e.getMessage());
            }
        }

        return new PaymentSummaryGetResponse(
                new SummaryDetailsResponse(defaultRequests, defaultAmount),
                new SummaryDetailsResponse(fallbackRequests, fallbackAmount)
        );
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
