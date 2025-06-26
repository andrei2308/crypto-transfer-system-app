package com.example.crypto_payment_system.service.api;

import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit service interface for exchange rate API
 */
public interface ExchangeRateApiService {
    @GET("/api/exchange-rate")
    Call<ExchangeRate> getExchangeRate();
}