package com.example.crypto_payment_system.repositories.api;

import android.util.Log;

import com.example.crypto_payment_system.config.auth.TokenManager;
import com.example.crypto_payment_system.domain.stripe.CreatePaymentIntentRequest;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentResponse;
import com.example.crypto_payment_system.domain.stripe.PaymentIntentStatusResponse;
import com.example.crypto_payment_system.service.api.StripeApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StripeRepositoryImpl implements StripeRepository {
    private static final String TAG = "StripeRepository";
    private final StripeApiService apiService;
    private final Gson gson;
    private final TokenManager tokenManager;
    private final AuthRepository authRepository;
    private final String serviceUsername;
    private final String servicePassword;

    public StripeRepositoryImpl(String baseUrl,
                                TokenManager tokenManager,
                                AuthRepository authRepository,
                                String serviceUsername,
                                String servicePassword) {
        this.tokenManager = tokenManager;
        this.authRepository = authRepository;
        this.serviceUsername = serviceUsername;
        this.servicePassword = servicePassword;

        this.gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, "API: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                if (!tokenManager.isTokenValid()) {
                    try {
                        authRepository.login(serviceUsername, servicePassword).get();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to refresh token", e);
                        throw new IOException("Authentication failed");
                    }
                }

                String token = tokenManager.getToken();
                if (token == null) {
                    throw new IOException("No authentication token available");
                }

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
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
                    Log.e(TAG, "Failed to create payment intent: " + response.code());
                    future.completeExceptionally(new Exception("Failed to create payment intent"));
                }
            }

            @Override
            public void onFailure(Call<PaymentIntentResponse> call, Throwable t) {
                Log.e(TAG, "Network error creating payment intent", t);
                future.completeExceptionally(t);
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
                    Log.e(TAG, "Failed to get payment intent status: " + response.code());
                    future.completeExceptionally(new Exception("Failed to get payment intent status"));
                }
            }

            @Override
            public void onFailure(Call<PaymentIntentStatusResponse> call, Throwable t) {
                Log.e(TAG, "Network error getting payment intent status", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}