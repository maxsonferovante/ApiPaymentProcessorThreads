package com.maal.apipaymentprocessorthreads.config;

import com.maal.apipaymentprocessorthreads.domain.document.PaymentDocument;
import com.maal.apipaymentprocessorthreads.domain.document.PaymentProcessorType;
import com.maal.apipaymentprocessorthreads.domain.model.Payment;
import com.maal.apipaymentprocessorthreads.domain.model.PaymentsProcess;
import com.maal.apipaymentprocessorthreads.entrypoint.dto.PaymentRequest;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({PaymentDocument.class, PaymentProcessorType.class, PaymentRequest.class, Payment.class, PaymentsProcess.class})
public class NativeConfig {
}
