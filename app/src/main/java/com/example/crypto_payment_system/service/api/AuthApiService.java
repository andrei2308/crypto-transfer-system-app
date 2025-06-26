package com.example.crypto_payment_system.service.api;

import com.example.crypto_payment_system.domain.auth.LoginRequest;
import com.example.crypto_payment_system.domain.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

}
