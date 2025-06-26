package com.example.crypto_payment_system.repositories.api;

import android.util.Log;

import com.example.crypto_payment_system.config.auth.TokenManager;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;
import com.example.crypto_payment_system.service.api.ExchangeRateApiService;
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

/**
 * Implementation of ExchangeRateRepository that fetches data from the API
 */
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {

    private static final String TAG = "ExchangeRateRepo";
    private final ExchangeRateApiService apiService;
    private final Gson gson;
    private final TokenManager tokenManager;
    private final AuthRepository authRepository;
    private final String serviceUsername;
    private final String servicePassword;

    public ExchangeRateRepositoryImpl(String baseUrl,
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

        this.apiService = retrofit.create(ExchangeRateApiService.class);
    }

    @Override
    public CompletableFuture<ExchangeRate> getExchangeRate() {
        CompletableFuture<ExchangeRate> future = new CompletableFuture<>();

        apiService.getExchangeRate().enqueue(new Callback<ExchangeRate>() {
            @Override
            public void onResponse(Call<ExchangeRate> call, Response<ExchangeRate> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Exchange rate fetched successfully");
                    future.complete(response.body());
                } else {
                    Log.e(TAG, "Failed to get exchange rate: " + response.code());
                    future.completeExceptionally(new Exception("Failed to get exchange rate"));
                }
            }

            @Override
            public void onFailure(Call<ExchangeRate> call, Throwable t) {
                Log.e(TAG, "Network error getting exchange rate", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}