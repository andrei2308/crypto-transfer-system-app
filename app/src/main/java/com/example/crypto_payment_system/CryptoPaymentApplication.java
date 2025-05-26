package com.example.crypto_payment_system;

import android.app.Application;

import com.example.crypto_payment_system.utils.currency.CurrencyManager;

/**
 * Custom Application class for global initialization
 */
public class CryptoPaymentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CurrencyManager.initialize(this);
    }
} 