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