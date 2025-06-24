package com.example.crypto_payment_system.repositories.api;

import com.example.crypto_payment_system.domain.stripe.CreatePaymentIntentRequest;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentResponse;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentStatusResponse;

import java.util.concurrent.CompletableFuture;

public interface StripeRepository {
    CompletableFuture<PaymentIntentResponse> createPaymentIntent(CreatePaymentIntentRequest request);

    CompletableFuture<PaymentIntentStatusResponse> getPaymentIntentStatus(String paymentIntentId);
}
