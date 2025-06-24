package com.example.crypto_payment_system.repositories.api;

import android.util.Log;

import com.example.crypto_payment_system.domain.stripe.CreatePaymentIntentRequest;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentResponse;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentStatusResponse;
import com.example.crypto_payment_system.service.api.StripeApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Singleton
public class StripeRepositoryImpl implements StripeRepository {

    private static final String TAG = "StripeRepository";
    private final StripeApiService apiService;
    private final Gson gson;

    @Inject
    public StripeRepositoryImpl(String baseUrl, String username, String password) {
        this.gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, "API: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Basic Auth Interceptor
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", Credentials.basic(username, password))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(original.method(), original.body());

            Request request = requestBuilder.build();
            return chain.proceed(request);
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.apiService = retrofit.create(StripeApiService.class);
    }

    @Override
    public CompletableFuture<PaymentIntentResponse> createPaymentIntent(CreatePaymentIntentRequest request) {
        CompletableFuture<PaymentIntentResponse> future = new CompletableFuture<>();

        apiService.createPaymentIntent(request).enqueue(new Callback<PaymentIntentResponse>() {
            @Override
            public void onResponse(Call<PaymentIntentResponse> call, Response<PaymentIntentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Payment intent created successfully: " + response.body().getPaymentIntentId());
                    future.complete(response.body());
                } else {
                    Log.e(TAG, "Failed to create payment intent");
                    future.completeExceptionally(new Exception());
                }
            }

            @Override
            public void onFailure(Call<PaymentIntentResponse> call, Throwable t) {
                Log.e(TAG, "Network error creating payment intent", t);
                future.completeExceptionally(new Exception());
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<PaymentIntentStatusResponse> getPaymentIntentStatus(String paymentIntentId) {
        CompletableFuture<PaymentIntentStatusResponse> future = new CompletableFuture<>();

        apiService.getPaymentIntentStatus(paymentIntentId).enqueue(new Callback<PaymentIntentStatusResponse>() {
            @Override
            public void onResponse(Call<PaymentIntentStatusResponse> call, Response<PaymentIntentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    future.complete(response.body());
                } else {
                    Log.e(TAG, "Failed to get payment intent status");
                    future.completeExceptionally(new Exception());
                }
            }

            @Override
            public void onFailure(Call<PaymentIntentStatusResponse> call, Throwable t) {
                Log.e(TAG, "Network error getting payment intent status", t);
                future.completeExceptionally(new Exception());
            }
        });

        return future;
    }
}