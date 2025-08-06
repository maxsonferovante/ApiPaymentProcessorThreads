# Api Payment Processor Threads

Este projeto é o serviço de processamento de pagamentos para a [Rinha de Backend 2025](https://github.com/zanfranceschi/rinha-de-backend-2025).

## Funcionalidades

* Recebe requisições de pagamento via HTTP REST.
* Processa pagamentos utilizando serviços externos (Payment Processors Default e Fallback).
* Gerencia a saúde dos Payment Processors através de verificações periódicas.
* Implementa mecanismo de failover automático entre instâncias.
* Utiliza Virtual Threads para alta concorrência.
* Armazena dados de pagamento no MongoDB.

## Tecnologias Principais

* Java 21
* Spring Boot
* MongoDB (para persistência)
* Virtual Threads (para alta concorrência)
* GraalVM Native Image
* Undertow (servidor HTTP otimizado)

## Arquitetura da Solução

### Visão Geral do Fluxo

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Cliente       │    │   Load Balancer  │    │   API Instances │
│   (Teste)       │───▶│   (Nginx)        │───▶│   (2x)          │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                          │
                                                          ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MongoDB       │◀───│   Payment        │◀───│   Virtual       │
│   (Persistência)│    │   Service        │    │   Threads       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                          │
                                                          ▼
                                               ┌─────────────────┐
                                               │   Payment       │
                                               │   Processors    │
                                               │   (Default +    │
                                               │   Fallback)     │
                                               └─────────────────┘
```

### Fluxo Detalhado de Processamento

#### 1. Recebimento da Requisição
```
POST /payments
{
    "correlationId": "uuid-unico",
    "amount": 100.00
}
```

#### 2. Processamento Assíncrono
```java
// PaymentController recebe a requisição
paymentService.paymentRequest(request);

// PaymentService adiciona à fila de processamento
paymentsQueue.addToQueue(new PaymentsProcess(paymentJson, payment, 0));
```

#### 3. Workers com Virtual Threads
```java
// 20 Virtual Threads processam pagamentos em paralelo
for (int i = 0; i < maxVirtualThreads; i++) {
    executor.submit(this::runWorker);
}
```

#### 4. Estratégia de Processamento
```java
// Tenta Default Processor primeiro
if (paymentProcessorDefaultClient.processPayment(paymentJson)) {
    savePayment(payment, PaymentProcessorType.DEFAULT);
    return;
}

// Se falhar, tenta Fallback Processor
if (paymentProcessorFallbackClient.processPayment(paymentJson)) {
    savePayment(payment, PaymentProcessorType.FALLBACK);
    return;
}

// Se ambos falharem, re-queue com retry
if (retryCount < maxRetries) {
    payment.incrementRetryCount();
    paymentsQueue.addToLastQueue(payment);
}
```

#### 5. Persistência no MongoDB
```java
// Inserção direta no MongoDB
PaymentDocument doc = new PaymentDocument();
doc.setCorrelationId(correlationId);
doc.setAmount(amount);
doc.setProcessorType(type);
mongoTemplate.insert(doc, "payments");
```

#### 6. Consulta de Resumo
```java
// Queries otimizadas por processor type e período
List<PaymentDocument> defaultPayments = 
    paymentPersistence.findByProcessorTypeAndRequestedAtBetween(
        "DEFAULT", from, to);
```

## Componentes Principais

### 1. PaymentController
- **Responsabilidade**: Recebe requisições HTTP
- **Endpoints**: `/payments`, `/payments-summary`, `/health`
- **Característica**: Resposta imediata (não bloqueante)

### 2. PaymentService
- **Responsabilidade**: Orquestra o processamento de pagamentos
- **Virtual Threads**: 20 workers em paralelo
- **Retry Logic**: Até 6 tentativas com backoff
- **Failover**: Default → Fallback → Re-queue

### 3. PaymentPriorityBlockingQueue
- **Responsabilidade**: Fila de processamento com prioridade
- **Implementação**: PriorityBlockingQueue
- **Prioridade**: Baseada no número de retries

### 4. PaymentSummaryService
- **Responsabilidade**: Agregação e consulta de dados
- **Queries Otimizadas**: Por processor type e período
- **Fallback**: Agregação MongoDB se queries falharem

### 5. MongoDB
- **Responsabilidade**: Persistência centralizada
- **Índices**: Compound indexes para performance


## Configurações de Ambiente

### Variáveis de Ambiente
```bash
# MongoDB
MONGODB_URI=mongodb://mongodb:27017/rinha

# Payment Processors
PAYMENT_PROCESSOR_DEFAULT_URL=http://payment-processor-default:8080
PAYMENT_PROCESSOR_FALLBACK_URL=http://payment-processor-fallback:8080

# Performance
PAYMENT_PROCESSOR_MAX_VIRTUAL_THREADS=20
PAYMENT_PROCESSOR_MAX_RETRIES=6
```

### Docker Compose
```yaml
services:
  api-payment-1:
    environment:
      MONGODB_URI: mongodb://mongodb:27017/rinha
      PAYMENT_PROCESSOR_MAX_VIRTUAL_THREADS: 20
      PAYMENT_PROCESSOR_MAX_RETRIES: 6
    deploy:
      resources:
        limits:
          cpus: "0.4"
          memory: "80MB"
```

## Como Rodar

Para executar este serviço (geralmente em conjunto com os Payment Processors e outros componentes), utilize o Docker Compose:

```bash
docker-compose up --build
```

## Endpoints

* `POST /payments` - Recebe requisições de pagamento
* `GET /payments-summary` - Retorna resumo dos pagamentos processados
* `GET /health` - Status de saúde dos Payment Processors
* `POST /purge-payments` - Limpa dados de pagamento (desenvolvimento)