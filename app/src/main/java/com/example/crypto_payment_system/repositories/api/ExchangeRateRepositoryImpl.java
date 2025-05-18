package com.example.crypto_payment_system.repositories.api;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.crypto_payment_system.config.ApiConfig;
import com.example.crypto_payment_system.domain.exchangeRate.ExchangeRate;
import com.example.crypto_payment_system.service.api.ExchangeRateApiService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of ExchangeRateRepository that fetches data from the API
 */
@Singleton
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {

    private static final String TAG = "ExchangeRateRepo";
    private final ExchangeRateApiService apiService;
    private final String authHeader;
    private final Gson gson;

    @Inject
    public ExchangeRateRepositoryImpl(String baseUrl, String username, String password) {
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, "API Response: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", Credentials.basic(username, password))
                    .header("Accept", "application/json")
                    .method(original.method(), original.body());
            
            Request request = requestBuilder.build();
            return chain.proceed(request);
        };
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.apiService = retrofit.create(ExchangeRateApiService.class);

        String credentials = username + ":" + password;
        byte[] credentialsBytes = credentials.getBytes(StandardCharsets.UTF_8);
        String base64Credentials = Base64.encodeToString(credentialsBytes, Base64.NO_WRAP);
        this.authHeader = "Basic " + base64Credentials;
    }

    @Override
    public CompletableFuture<ExchangeRate> getExchangeRate() {
        CompletableFuture<ExchangeRate> future = new CompletableFuture<>();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        String fullUrl = ApiConfig.BASE_URL + "rate";

        String credentials = Credentials.basic(ApiConfig.USERNAME, ApiConfig.PASSWORD);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", credentials)
                .addHeader("Accept", "application/json")
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            if (jsonObject.has("eurUsd")) {
                                ExchangeRate rate = new ExchangeRate();
                                rate.setRate(jsonObject.getDouble("eurUsd"));
                                rate.setFromCurrency("EUR");
                                rate.setToCurrency("USD");

                                if (jsonObject.has("lastUpdated")) {
                                    long timestamp = jsonObject.getLong("lastUpdated");
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                    String formattedDate = sdf.format(new Date(timestamp));
                                    rate.setTimestamp(formattedDate);
                                } else {
                                    rate.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                            .format(new Date()));
                                }
                                future.complete(rate);
                                return;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing specific JSON format", e);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response from " + fullUrl, e);
                    }
                } else {
                    Log.w(TAG, "Non-successful response from " + fullUrl + ": " + response.code());
                }
            }
        });
        return future;
    }
} 