package com.example.crypto_payment_system.repositories.api;

import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;

import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for fetching exchange rate data
 */
public interface ExchangeRateRepository {
    
    /**
     * Get the current exchange rate from the API
     * @return CompletableFuture with the exchange rate data
     */
    CompletableFuture<ExchangeRate> getExchangeRate();
} 