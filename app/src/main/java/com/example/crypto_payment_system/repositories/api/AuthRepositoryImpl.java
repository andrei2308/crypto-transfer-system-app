package com.example.crypto_payment_system.repositories.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.crypto_payment_system.config.auth.TokenManager;
import com.example.crypto_payment_system.domain.auth.LoginRequest;
import com.example.crypto_payment_system.domain.auth.LoginResponse;
import com.example.crypto_payment_system.service.api.AuthApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepository";
    private final AuthApiService apiService;
    private final TokenManager tokenManager;

    public AuthRepositoryImpl(String baseUrl, TokenManager tokenManager) {
        this.tokenManager = tokenManager;

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, "API: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
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

        this.apiService = retrofit.create(AuthApiService.class);
    }

    @Override
    public CompletableFuture<LoginResponse> login(String username, String password) {
        CompletableFuture<LoginResponse> future = new CompletableFuture<>();

        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    long expiryTimeMillis = System.currentTimeMillis() +
                            (loginResponse.getExpiresIn() * 1000);
                    tokenManager.saveToken(loginResponse.getToken(), expiryTimeMillis);

                    Log.d(TAG, "Login successful, token saved");
                    future.complete(loginResponse);
                } else {
                    Log.e(TAG, "Login failed: " + response.code());
                    future.completeExceptionally(new Exception("Login failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error during login", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}
