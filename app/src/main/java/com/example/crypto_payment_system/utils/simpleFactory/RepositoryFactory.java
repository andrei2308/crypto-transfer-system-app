package com.example.crypto_payment_system.utils.simpleFactory;

import android.content.Context;

import com.example.crypto_payment_system.config.auth.TokenManager;
import com.example.crypto_payment_system.repositories.api.AuthRepository;
import com.example.crypto_payment_system.repositories.api.AuthRepositoryImpl;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepository;
import com.example.crypto_payment_system.repositories.api.ExchangeRateRepositoryImpl;
import com.example.crypto_payment_system.repositories.api.StripeRepository;
import com.example.crypto_payment_system.repositories.api.StripeRepositoryImpl;

public class RepositoryFactory {

    public static StripeRepository createStripeRepository(
            Context context,
            String baseUrl,
            String serviceUsername,
            String servicePassword) {

        TokenManager tokenManager = new TokenManager(context);
        AuthRepository authRepository = new AuthRepositoryImpl(baseUrl, tokenManager);

        return new StripeRepositoryImpl(
                baseUrl,
                tokenManager,
                authRepository,
                serviceUsername,
                servicePassword
        );
    }

    public static ExchangeRateRepository createExchangeRepository(
            Context context,
            String baseUrl,
            String serviceUsername,
            String servicePassword
    ) {
        TokenManager tokenManager = new TokenManager(context);
        AuthRepository authRepository = new AuthRepositoryImpl(baseUrl, tokenManager);
        return new ExchangeRateRepositoryImpl(
                baseUrl,
                tokenManager,
                authRepository,
                serviceUsername,
                servicePassword);
    }

}
