#!/usr/bin/env python3


import requests
import json
import uuid
import time
import threading
import random
from concurrent.futures import ThreadPoolExecutor, as_completed

# Configurações
BASE_URL = "http://localhost:9999"
NUM_REQUESTS = 15000
NUM_THREADS = 50

def send_payment():
    """Envia uma requisição de pagamento"""
    correlation_id = str(uuid.uuid4())
    amount = round(random.uniform(10.0, 1000.0), 2)
    
    payload = {
        "correlationId": correlation_id,
        "amount": amount
    }
    
    try:
        start_time = time.time()
        response = requests.post(
            f"{BASE_URL}/payments",
            json=payload,
            timeout=10
        )
        end_time = time.time()
        
        return {
            "status_code": response.status_code,
            "response_time": end_time - start_time,
            "correlation_id": correlation_id,
            "amount": amount,
            "success": response.status_code == 200
        }
    except Exception as e:
        return {
            "status_code": 0,
            "response_time": 0,
            "correlation_id": correlation_id,
            "amount": amount,
            "success": False,
            "error": str(e)
        }

def get_summary():
    """Obtém o resumo de pagamentos"""
    try:
        response = requests.get(f"{BASE_URL}/payments-summary", timeout=10)
        if response.status_code == 200:
            return response.json()
        else:
            return None
    except Exception as e:
        print(f"Erro ao obter summary: {e}")
        return None

def main():
    print(f"Iniciando teste de carga com {NUM_REQUESTS} requisições usando {NUM_THREADS} threads")
    print(f"URL base: {BASE_URL}")
    
    # Verificar se o serviço está disponível
    try:
        response = requests.get(f"{BASE_URL}/payments-summary", timeout=5)
        if response.status_code != 200:
            print("Serviço não está disponível!")
            return
    except Exception as e:
        print(f"Erro ao conectar com o serviço: {e}")
        return
    
    # Obter summary inicial
    initial_summary = get_summary()
    print(f"Summary inicial: {initial_summary}")
    
    # Executar requisições em paralelo
    start_time = time.time()
    results = []
    
    with ThreadPoolExecutor(max_workers=NUM_THREADS) as executor:
        futures = [executor.submit(send_payment) for _ in range(NUM_REQUESTS)]
        
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            
            if len(results) % 10 == 0:
                print(f"Processadas {len(results)}/{NUM_REQUESTS} requisições")
    
    end_time = time.time()
    total_time = end_time - start_time
    
    # Aguardar processamento assíncrono
    print("Aguardando processamento assíncrono...")
    time.sleep(5)
    
    # Obter summary final
    final_summary = get_summary()
    print(f"Summary final: {final_summary}")
    
    # Calcular estatísticas
    successful_requests = [r for r in results if r["success"]]
    failed_requests = [r for r in results if not r["success"]]
    
    if successful_requests:
        response_times = [r["response_time"] for r in successful_requests]
        avg_response_time = sum(response_times) / len(response_times)
        min_response_time = min(response_times)
        max_response_time = max(response_times)
    else:
        avg_response_time = min_response_time = max_response_time = 0
    
    total_amount = sum(r["amount"] for r in successful_requests)
    
    print("\n" + "="*50)
    print("RESULTADOS DO TESTE DE CARGA")
    print("="*50)
    print(f"Total de requisições: {NUM_REQUESTS}")
    print(f"Requisições bem-sucedidas: {len(successful_requests)}")
    print(f"Requisições falhadas: {len(failed_requests)}")
    print(f"Taxa de sucesso: {len(successful_requests)/NUM_REQUESTS*100:.2f}%")
    print(f"Tempo total: {total_time:.2f}s")
    print(f"Requisições por segundo: {NUM_REQUESTS/total_time:.2f}")
    print(f"Tempo médio de resposta: {avg_response_time*1000:.2f}ms")
    print(f"Tempo mínimo de resposta: {min_response_time*1000:.2f}ms")
    print(f"Tempo máximo de resposta: {max_response_time*1000:.2f}ms")
    print(f"Valor total enviado: R$ {total_amount:.2f}")
    
    if final_summary:
        processed_default = final_summary["default"]["totalRequests"]
        processed_fallback = final_summary["fallback"]["totalRequests"]
        processed_total = processed_default + processed_fallback
        
        print(f"\nProcessamento pelos Payment Processors:")
        print(f"Default: {processed_default} requisições")
        print(f"Fallback: {processed_fallback} requisições")
        print(f"Total processado: {processed_total}")
        print(f"Taxa de processamento: {processed_total/len(successful_requests)*100:.2f}%")
    
    if failed_requests:
        print(f"\nErros encontrados:")
        error_types = {}
        for req in failed_requests:
            error = req.get("error", "Unknown")
            error_types[error] = error_types.get(error, 0) + 1
        
        for error, count in error_types.items():
            print(f"  {error}: {count} ocorrências")

if __name__ == "__main__":
    main()

