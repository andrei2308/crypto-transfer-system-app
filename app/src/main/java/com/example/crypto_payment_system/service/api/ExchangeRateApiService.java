package com.example.crypto_payment_system.service.api;

import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 * Retrofit service interface for exchange rate API
 */
public interface ExchangeRateApiService {
    
    /**
     * Get exchange rates with Basic Authentication
     * @param authHeader Authorization header value (Basic base64(username:password))
     * @return Exchange rate data
     */
    @GET(ApiConfig.EXCHANGE_RATE_ENDPOINT)
    Call<ExchangeRate> getExchangeRate(@Header("Authorization") String authHeader);
} 