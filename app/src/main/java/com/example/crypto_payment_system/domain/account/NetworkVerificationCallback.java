package com.example.crypto_payment_system.domain.account;

public interface NetworkVerificationCallback {
    void onSuccess(String walletAddress, AccountNetworkInfo networkInfo);
    void onError(String error);
}
