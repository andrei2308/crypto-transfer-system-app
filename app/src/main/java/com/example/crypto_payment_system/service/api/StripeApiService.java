package com.example.crypto_payment_system.service.api;

import com.example.crypto_payment_system.domain.stripe.CreatePaymentIntentRequest;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentResponse;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface StripeApiService {

    @POST("/api/v1/stripe/payment-intent")
    Call<PaymentIntentResponse> createPaymentIntent(@Body CreatePaymentIntentRequest request);

    @GET("/api/v1/stripe/payment-intent/{id}")
    Call<PaymentIntentStatusResponse> getPaymentIntentStatus(@Path("id") String paymentIntentId);
}
