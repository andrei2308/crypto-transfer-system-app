package com.example.crypto_payment_system.config;

import com.example.crypto_payment_system.BuildConfig;

/**
 * Configuration class for API settings
 */
public class ApiConfig {
    public static final String BASE_URL = "http://10.0.2.2:8080/";
    
    public static final String USERNAME = BuildConfig.BACKEND_USERNAME;
    public static final String PASSWORD = BuildConfig.BACKEND_PASSWORD;

    public static final String EXCHANGE_RATE_ENDPOINT = "api/rates";
} 