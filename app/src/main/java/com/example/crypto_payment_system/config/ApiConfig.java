package com.example.crypto_payment_system.config;

import com.example.crypto_payment_system.BuildConfig;

/**
 * Configuration class for API settings
 */
public class ApiConfig {
    public static final String BASE_URL = "https://exchange-rates-backend-me3v.onrender.com/";
    
    public static final String USERNAME = BuildConfig.BACKEND_USERNAME;
    public static final String PASSWORD = BuildConfig.BACKEND_PASSWORD;

    public static final String EXCHANGE_RATE_ENDPOINT = "api/rates";
} 